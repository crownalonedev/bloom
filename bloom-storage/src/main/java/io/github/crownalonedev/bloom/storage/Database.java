package io.github.crownalonedev.bloom.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.crownalonedev.bloom.api.lifecycle.Terminable;
import io.github.crownalonedev.bloom.storage.internal.Jdbc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A pooled SQL database with an asynchronous API.
 *
 * <pre>{@code
 * Database db = Database.builder()
 *         .sqlite(getDataFolder().toPath().resolve("data.db"))
 *         .mainThreadCheck(bloom.scheduler()::isPrimaryThread)
 *         .logger(getLogger())
 *         .build();
 * db.migrate(migrations).join();          // acceptable in onEnable: the server is not ticking yet
 *
 * db.queryOne("SELECT kills FROM stats WHERE uuid = ?", rs -> rs.getInt(1), player.getUniqueId())
 *         .thenAcceptAsync(kills -> player.sendMessage("Kills: " + kills.orElse(0)),
 *                 bloom.scheduler().mainThreadExecutor());   // back on the main thread
 * }</pre>
 *
 * <h2>Threading</h2>
 * Operations run on a bounded pool of database threads (one per connection). Returned futures complete on those
 * threads: switch to the main thread with {@code thenAcceptAsync(..., mainThreadExecutor())} before touching game
 * state. When the queue is full (default 1000 pending operations) new operations fail immediately with
 * {@link StorageException} instead of piling up unbounded work.
 *
 * <h2>Shutdown</h2>
 * {@link #close()} stops accepting work, waits for queued operations (default up to 10 s), fails any operation that
 * could not run, and closes the pool. Call it in {@code onDisable}; when a Bloom instance owns it (registered in
 * the service registry) Bloom closes it automatically.
 *
 * @since 0.1.0
 */
public final class Database implements Terminable {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final HikariDataSource dataSource;
    private final ThreadPoolExecutor executor;
    private final Dialect dialect;
    private final BooleanSupplier mainThread;
    private final String migrationTable;
    private final Logger logger;
    private final Duration shutdownTimeout;
    private final BlockingDatabase blocking;
    private volatile boolean closed;

    private Database(Builder b, HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.dialect = b.dialect;
        this.mainThread = b.mainThread;
        this.migrationTable = b.migrationTable;
        this.logger = b.logger;
        this.shutdownTimeout = b.shutdownTimeout;
        final String poolName = b.poolName;
        final AtomicInteger counter = new AtomicInteger();
        this.executor = new ThreadPoolExecutor(
                b.poolSize,
                b.poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(b.queueCapacity),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable, poolName + "-db-" + counter.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.AbortPolicy());
        this.blocking = new BlockingDatabase(dataSource, mainThread);
    }

    /**
     * Starts configuring a database.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the dialect.
     *
     * @return dialect
     */
    public Dialect dialect() {
        return dialect;
    }

    /**
     * Returns synchronous access for code already running off the main thread.
     *
     * @return blocking API
     */
    public BlockingDatabase blocking() {
        return blocking;
    }

    /**
     * Returns the number of queued operations that have not started yet.
     *
     * @return queue size
     */
    public int pendingOperations() {
        return executor.getQueue().size();
    }

    // ---- async operations ---------------------------------------------------------------------------------

    /**
     * Executes an INSERT/UPDATE/DELETE/DDL statement asynchronously.
     *
     * @param sql statement
     * @param params parameters (see {@link SqlOperations} for supported types)
     * @return future with the affected row count
     */
    public CompletableFuture<Integer> update(final String sql, final Object... params) {
        return submit(c -> Jdbc.update(c, sql, params));
    }

    /**
     * Executes an INSERT asynchronously and returns the generated key.
     *
     * @param sql statement
     * @param params parameters
     * @return future with the generated key, if any
     */
    public CompletableFuture<Optional<Long>> insert(final String sql, final Object... params) {
        return submit(c -> Jdbc.insert(c, sql, params));
    }

    /**
     * Executes a JDBC batch asynchronously.
     *
     * @param sql statement
     * @param rows parameter rows
     * @return future with per-row update counts
     */
    public CompletableFuture<int[]> batch(final String sql, final List<Object[]> rows) {
        final List<Object[]> copy = new ArrayList<Object[]>(rows);
        return submit(c -> Jdbc.batch(c, sql, copy));
    }

    /**
     * Runs a query asynchronously.
     *
     * @param sql query
     * @param mapper row mapper (runs on a database thread)
     * @param params parameters
     * @param <T> row type
     * @return future with all mapped rows
     */
    public <T> CompletableFuture<List<T>> query(final String sql, final RowMapper<T> mapper, final Object... params) {
        return submit(c -> Jdbc.query(c, sql, mapper, params));
    }

    /**
     * Runs a single-row query asynchronously.
     *
     * @param sql query
     * @param mapper row mapper (runs on a database thread)
     * @param params parameters
     * @param <T> row type
     * @return future with the first row, or empty
     */
    public <T> CompletableFuture<Optional<T>> queryOne(
            final String sql, final RowMapper<T> mapper, final Object... params) {
        return submit(c -> Jdbc.queryOne(c, sql, mapper, params));
    }

    /**
     * Runs work in a transaction and returns its result. Commits when {@code work} returns, rolls back if it throws.
     *
     * @param work transaction body (runs on a database thread)
     * @param <T> result type
     * @return future with the result
     */
    public <T> CompletableFuture<T> transaction(final Function<Transaction, T> work) {
        Objects.requireNonNull(work, "work");
        return submit(c -> inTransaction(c, work));
    }

    /**
     * Runs work without a result in a transaction.
     *
     * @param work transaction body (runs on a database thread)
     * @return future completed when committed
     */
    public CompletableFuture<Void> inTransaction(final Consumer<Transaction> work) {
        Objects.requireNonNull(work, "work");
        return transaction(tx -> {
            work.accept(tx);
            return null;
        });
    }

    /**
     * Gives raw JDBC access on a database thread. The connection is returned to the pool afterwards.
     *
     * @param work connection callback
     * @param <T> result type
     * @return future with the result
     */
    public <T> CompletableFuture<T> withConnection(final ConnectionWork<T> work) {
        Objects.requireNonNull(work, "work");
        return submit(c -> {
            try {
                return work.apply(c);
            } catch (SQLException e) {
                throw new StorageException("Database operation failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Applies pending migrations in version order, recording them in the history table (default
     * {@code bloom_schema_history}; configurable with {@link Builder#migrationTable(String)}).
     *
     * @param migrations all migrations of the plugin
     * @return future with the result; fails if a migration fails or the database is newer than the migrations
     */
    public CompletableFuture<MigrationResult> migrate(final List<Migration> migrations) {
        final List<Migration> sorted = new ArrayList<Migration>(migrations);
        Collections.sort(sorted, (a, b) -> Integer.compare(a.version(), b.version()));
        Set<Integer> seen = new HashSet<Integer>();
        for (Migration migration : sorted) {
            if (!seen.add(migration.version())) {
                throw new IllegalArgumentException("Duplicate migration version " + migration.version());
            }
        }
        return submit(c -> runMigrations(c, sorted));
    }

    /** Raw JDBC callback for {@link #withConnection(ConnectionWork)}. */
    @FunctionalInterface
    public interface ConnectionWork<T> {
        /**
         * Uses the connection.
         *
         * @param connection pooled connection; do not close it
         * @return result
         * @throws SQLException on JDBC errors
         */
        T apply(Connection connection) throws SQLException;
    }

    // ---- lifecycle ----------------------------------------------------------------------------------------

    /**
     * Stops accepting operations, waits for queued work, fails work that could not run and closes the pool.
     * Idempotent. Blocks for at most the configured shutdown timeout.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                List<Runnable> dropped = executor.shutdownNow();
                for (Runnable runnable : dropped) {
                    if (runnable instanceof Operation) {
                        ((Operation<?>) runnable)
                                .fail(new StorageException("Database closed before the operation could run."));
                    }
                }
                logger.warning("Database did not finish " + dropped.size() + " queued operation(s) within "
                        + shutdownTimeout.getSeconds() + "s of shutdown; they were cancelled.");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        dataSource.close();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    // ---- internals ----------------------------------------------------------------------------------------

    private interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final class Operation<T> implements Runnable {
        private final CompletableFuture<T> future = new CompletableFuture<T>();
        private final Work<T> work;

        Operation(Work<T> work) {
            this.work = work;
        }

        @Override
        public void run() {
            if (future.isDone()) {
                return;
            }
            Connection acquired;
            try {
                acquired = dataSource.getConnection();
            } catch (SQLException e) {
                future.completeExceptionally(
                        new StorageException("Could not obtain a database connection: " + e.getMessage(), e));
                return;
            }
            try (Connection connection = acquired) {
                future.complete(work.run(connection));
            } catch (SQLException e) {
                future.completeExceptionally(new StorageException("Database operation failed: " + e.getMessage(), e));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }

        void fail(Throwable error) {
            future.completeExceptionally(error);
        }
    }

    private <T> CompletableFuture<T> submit(Work<T> work) {
        Operation<T> operation = new Operation<T>(work);
        if (closed) {
            operation.fail(new StorageException("The database is closed."));
            return operation.future;
        }
        try {
            executor.execute(operation);
        } catch (RejectedExecutionException e) {
            operation.fail(
                    closed
                            ? new StorageException("The database is closed.")
                            : new StorageException("The database queue is full ("
                                    + executor.getQueue().size() + " pending "
                                    + "operations). The database may be slow or unreachable."));
        }
        return operation.future;
    }

    private <T> T inTransaction(Connection connection, Function<Transaction, T> work) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        TransactionImpl tx = new TransactionImpl(connection, dialect);
        try {
            T result = work.apply(tx);
            connection.commit();
            return result;
        } catch (RuntimeException | Error e) {
            rollbackQuietly(connection, e);
            throw e;
        } finally {
            tx.finish();
            connection.setAutoCommit(autoCommit);
        }
    }

    private MigrationResult runMigrations(Connection connection, List<Migration> migrations) throws SQLException {
        Jdbc.update(
                connection,
                "CREATE TABLE IF NOT EXISTS " + migrationTable
                        + " (version INT NOT NULL PRIMARY KEY, description VARCHAR(255) NOT NULL, applied_at BIGINT NOT NULL)",
                null);
        TreeSet<Integer> applied = new TreeSet<Integer>(
                Jdbc.query(connection, "SELECT version FROM " + migrationTable, rs -> rs.getInt(1), null));
        int previous = applied.isEmpty() ? 0 : applied.last();
        int newest =
                migrations.isEmpty() ? 0 : migrations.get(migrations.size() - 1).version();
        if (previous > newest) {
            throw new StorageException("The database schema is at version " + previous + ", newer than the newest "
                    + "migration this plugin knows (" + newest + "). Was the plugin downgraded? Refusing to continue.");
        }
        List<Integer> ran = new ArrayList<Integer>();
        for (final Migration migration : migrations) {
            if (applied.contains(migration.version())) {
                continue;
            }
            try {
                inTransaction(connection, tx -> {
                    migration.step().apply(tx);
                    tx.update(
                            "INSERT INTO " + migrationTable + " (version, description, applied_at) VALUES (?, ?, ?)",
                            migration.version(),
                            migration.description(),
                            System.currentTimeMillis());
                    return null;
                });
            } catch (RuntimeException e) {
                throw new StorageException(
                        "Database migration " + migration.version() + " ('" + migration.description() + "') failed: "
                                + e.getMessage(),
                        e);
            }
            applied.add(migration.version());
            ran.add(migration.version());
            logger.info("Applied database migration " + migration.version() + ": " + migration.description());
        }
        return new MigrationResult(previous, applied.isEmpty() ? 0 : applied.last(), ran);
    }

    private void rollbackQuietly(Connection connection, Throwable cause) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            cause.addSuppressed(e);
            logger.log(Level.WARNING, "Rollback failed", e);
        }
    }

    static String identifier(String name, String what) {
        if (name == null || !IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid " + what + " '" + name + "': use letters, digits and '_' "
                    + "(starting with a letter or '_').");
        }
        return name;
    }

    private static final class TransactionImpl implements Transaction {
        private final Connection connection;
        private final Dialect dialect;
        private volatile boolean finished;

        TransactionImpl(Connection connection, Dialect dialect) {
            this.connection = connection;
            this.dialect = dialect;
        }

        void finish() {
            finished = true;
        }

        private Connection active() {
            if (finished) {
                throw new IllegalStateException("This transaction has already completed; Transaction objects must "
                        + "not be used outside their callback.");
            }
            return connection;
        }

        @Override
        public Dialect dialect() {
            return dialect;
        }

        @Override
        public Connection connection() {
            return active();
        }

        @Override
        public int update(String sql, Object... params) {
            return Jdbc.update(active(), sql, params);
        }

        @Override
        public Optional<Long> insert(String sql, Object... params) {
            return Jdbc.insert(active(), sql, params);
        }

        @Override
        public int[] batch(String sql, List<Object[]> rows) {
            return Jdbc.batch(active(), sql, rows);
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
            return Jdbc.query(active(), sql, mapper, params);
        }

        @Override
        public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
            return Jdbc.queryOne(active(), sql, mapper, params);
        }
    }

    /** Builder for {@link Database}. */
    public static final class Builder {
        private Dialect dialect;
        private String jdbcUrl;
        private String username;
        private String password;
        private Path sqliteFile;
        private int poolSize = -1;
        private int queueCapacity = 1000;
        private Duration connectionTimeout = Duration.ofSeconds(10);
        private Duration maxLifetime = Duration.ofMinutes(30);
        private Duration shutdownTimeout = Duration.ofSeconds(10);
        private String poolName = "Bloom";
        private String migrationTable = "bloom_schema_history";
        private String driverClassName;
        private final Map<String, String> properties = new LinkedHashMap<String, String>();
        private BooleanSupplier mainThread = () -> false;
        private Logger logger = Logger.getLogger("Bloom-Storage");

        private Builder() {}

        /**
         * Uses an SQLite file. Parent directories are created. The pool has one connection by default, which
         * serialises writes and avoids {@code SQLITE_BUSY}.
         *
         * @param file database file
         * @return this builder
         */
        public Builder sqlite(Path file) {
            this.dialect = Dialect.SQLITE;
            this.sqliteFile = Objects.requireNonNull(file, "file").toAbsolutePath();
            this.jdbcUrl = "jdbc:sqlite:" + sqliteFile;
            return this;
        }

        /**
         * Uses MySQL.
         *
         * @param host host name
         * @param port port, usually 3306
         * @param database schema name
         * @param user user name
         * @param pass password
         * @return this builder
         */
        public Builder mysql(String host, int port, String database, String user, String pass) {
            return network(Dialect.MYSQL, "jdbc:mysql://", host, port, database, user, pass);
        }

        /**
         * Uses MariaDB (shade {@code org.mariadb.jdbc:mariadb-java-client} into your plugin).
         *
         * @param host host name
         * @param port port, usually 3306
         * @param database schema name
         * @param user user name
         * @param pass password
         * @return this builder
         */
        public Builder mariadb(String host, int port, String database, String user, String pass) {
            return network(Dialect.MARIADB, "jdbc:mariadb://", host, port, database, user, pass);
        }

        private Builder network(
                Dialect type, String scheme, String host, int port, String database, String user, String pass) {
            this.dialect = type;
            this.jdbcUrl = scheme + Objects.requireNonNull(host, "host") + ":" + port + "/"
                    + Objects.requireNonNull(database, "database");
            this.username = user;
            this.password = pass;
            return this;
        }

        /**
         * Uses a custom JDBC URL.
         *
         * @param url JDBC URL
         * @param user user name, may be {@code null}
         * @param pass password, may be {@code null}
         * @param type dialect of the database
         * @return this builder
         */
        public Builder jdbcUrl(String url, String user, String pass, Dialect type) {
            this.jdbcUrl = Objects.requireNonNull(url, "url");
            this.username = user;
            this.password = pass;
            this.dialect = Objects.requireNonNull(type, "type");
            return this;
        }

        /**
         * Sets the number of connections and database threads (default 1 for SQLite, 8 otherwise).
         *
         * @param size pool size, {@code >= 1}
         * @return this builder
         */
        public Builder poolSize(int size) {
            if (size < 1) {
                throw new IllegalArgumentException("Pool size must be >= 1");
            }
            this.poolSize = size;
            return this;
        }

        /**
         * Sets the maximum number of queued operations before new ones fail fast (default 1000).
         *
         * @param capacity queue capacity
         * @return this builder
         */
        public Builder queueCapacity(int capacity) {
            if (capacity < 1) {
                throw new IllegalArgumentException("Queue capacity must be >= 1");
            }
            this.queueCapacity = capacity;
            return this;
        }

        /**
         * Sets how long to wait for a free connection (default 10 s).
         *
         * @param timeout timeout
         * @return this builder
         */
        public Builder connectionTimeout(Duration timeout) {
            this.connectionTimeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        /**
         * Sets the maximum lifetime of pooled connections (default 30 min; keep it below the server's
         * {@code wait_timeout}).
         *
         * @param lifetime lifetime
         * @return this builder
         */
        public Builder maxLifetime(Duration lifetime) {
            this.maxLifetime = Objects.requireNonNull(lifetime, "lifetime");
            return this;
        }

        /**
         * Sets how long {@link Database#close()} waits for queued work (default 10 s).
         *
         * @param timeout timeout
         * @return this builder
         */
        public Builder shutdownTimeout(Duration timeout) {
            this.shutdownTimeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        /**
         * Sets the pool and thread name prefix (default {@code "Bloom"}); use your plugin name.
         *
         * @param name pool name
         * @return this builder
         */
        public Builder poolName(String name) {
            this.poolName = Objects.requireNonNull(name, "name");
            return this;
        }

        /**
         * Sets the migration history table (default {@code bloom_schema_history}). Plugins sharing one MySQL schema
         * must use different tables, e.g. {@code "myplugin_schema_history"}.
         *
         * @param table table name
         * @return this builder
         */
        public Builder migrationTable(String table) {
            this.migrationTable = identifier(table, "migration table name");
            return this;
        }

        /**
         * Forces a JDBC driver class instead of auto-detection.
         *
         * @param className driver class
         * @return this builder
         */
        public Builder driverClassName(String className) {
            this.driverClassName = className;
            return this;
        }

        /**
         * Adds a driver/data-source property (e.g. {@code useSSL=true}).
         *
         * @param key property
         * @param value value
         * @return this builder
         */
        public Builder property(String key, String value) {
            properties.put(key, value);
            return this;
        }

        /**
         * Tells the database how to detect the main server thread, so {@link BlockingDatabase} can refuse to block
         * it. With Bloom: {@code bloom.scheduler()::isPrimaryThread}.
         *
         * @param check returns {@code true} on the main thread
         * @return this builder
         */
        public Builder mainThreadCheck(BooleanSupplier check) {
            this.mainThread = Objects.requireNonNull(check, "check");
            return this;
        }

        /**
         * Sets the logger for migrations and shutdown messages.
         *
         * @param target logger, usually {@code plugin.getLogger()}
         * @return this builder
         */
        public Builder logger(Logger target) {
            this.logger = Objects.requireNonNull(target, "logger");
            return this;
        }

        /**
         * Opens the pool. Fails fast if the database is unreachable.
         *
         * @return the database
         * @throws StorageException with a diagnostic message if the pool cannot start
         */
        public Database build() {
            if (dialect == null) {
                throw new IllegalStateException(
                        "Choose a database with sqlite(...), mysql(...), mariadb(...) or " + "jdbcUrl(...).");
            }
            if (poolSize < 0) {
                poolSize = dialect == Dialect.SQLITE ? 1 : 8;
            }
            HikariConfig config = new HikariConfig();
            config.setPoolName(poolName);
            config.setJdbcUrl(jdbcUrl);
            if (username != null) {
                config.setUsername(username);
            }
            if (password != null) {
                config.setPassword(password);
            }
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(poolSize);
            config.setConnectionTimeout(connectionTimeout.toMillis());
            config.setMaxLifetime(maxLifetime.toMillis());
            config.setInitializationFailTimeout(1);
            String driver = resolveDriver();
            try {
                config.setDriverClassName(driver);
            } catch (RuntimeException e) {
                throw new StorageException(
                        "Could not connect: the JDBC driver class '" + driver + "' could not be "
                                + "loaded. Shade the driver into your plugin or fix driverClassName(...).",
                        e);
            }
            for (Map.Entry<String, String> entry : defaultProperties().entrySet()) {
                config.addDataSourceProperty(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                config.addDataSourceProperty(entry.getKey(), entry.getValue());
            }
            if (sqliteFile != null) {
                try {
                    Path parent = sqliteFile.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                } catch (java.io.IOException e) {
                    throw new StorageException("Could not create the directory for " + sqliteFile, e);
                }
            }
            HikariDataSource dataSource;
            try {
                dataSource = new HikariDataSource(config);
                if (dialect == Dialect.SQLITE && !properties.containsKey("journal_mode")) {
                    enableWal(dataSource);
                }
            } catch (RuntimeException e) {
                Throwable root = e;
                while (root.getCause() != null) {
                    root = root.getCause();
                }
                throw new StorageException(
                        "Could not connect to the " + dialect + " database at " + safeUrl() + ": "
                                + root.getMessage()
                                + ". Check the host, port, database name and credentials, and that the "
                                + "database server is running.",
                        e);
            }
            return new Database(this, dataSource);
        }

        private Map<String, String> defaultProperties() {
            Map<String, String> defaults = new LinkedHashMap<String, String>();
            if (dialect == Dialect.SQLITE) {
                defaults.put("foreign_keys", "true");
                defaults.put("busy_timeout", "5000");
                // journal_mode is not a connection property: sqlite-jdbc 3.7.2 (bundled with 1.8 servers) applies
                // properties as a batch and rejects pragmas that return rows. See enableWal().
            } else if (dialect == Dialect.MYSQL) {
                defaults.put("cachePrepStmts", "true");
                defaults.put("prepStmtCacheSize", "250");
                defaults.put("prepStmtCacheSqlLimit", "2048");
                defaults.put("useServerPrepStmts", "true");
                defaults.put("characterEncoding", "utf8");
            }
            return defaults;
        }

        /** WAL is stored in the database file, so setting it once covers every pooled connection. */
        private static void enableWal(HikariDataSource dataSource) {
            try (Connection connection = dataSource.getConnection();
                    java.sql.Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
            } catch (SQLException e) {
                dataSource.close();
                throw new IllegalStateException("Could not enable WAL journal mode: " + e.getMessage(), e);
            }
        }

        private String resolveDriver() {
            if (driverClassName != null) {
                return driverClassName;
            }
            for (String candidate : dialect.driverClasses()) {
                try {
                    Class.forName(candidate, false, Database.class.getClassLoader());
                    return candidate;
                } catch (ClassNotFoundException | LinkageError e) {
                    // try the next candidate
                }
            }
            throw new StorageException("No JDBC driver for " + dialect + " was found (tried " + dialect.driverClasses()
                    + "). Most servers bundle SQLite and MySQL drivers; for MariaDB, or if your server does not, "
                    + "shade the driver into your plugin.");
        }

        private String safeUrl() {
            int query = jdbcUrl.indexOf('?');
            return query < 0 ? jdbcUrl : jdbcUrl.substring(0, query);
        }
    }
}
