package io.github.crownalonedev.bloom.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseTest {

    @TempDir
    Path dir;

    private Database db;

    enum Rank {
        MEMBER,
        VIP
    }

    static final class Stats {
        final UUID id;
        final int kills;
        final Rank rank;

        Stats(UUID id, int kills, Rank rank) {
            this.id = id;
            this.kills = kills;
            this.rank = rank;
        }
    }

    static final class StatsRepository extends SqlRepository<UUID, Stats> {
        StatsRepository(Database database) {
            super(
                    database,
                    "stats",
                    "uuid",
                    Arrays.asList("uuid", "kills", "rank_name"),
                    rs -> new Stats(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getInt("kills"),
                            Rank.valueOf(rs.getString("rank_name"))));
        }

        @Override
        protected UUID keyOf(Stats value) {
            return value.id;
        }

        @Override
        protected Object[] valuesOf(Stats value) {
            return new Object[] {value.id, value.kills, value.rank};
        }
    }

    private static final List<Migration> MIGRATIONS = Arrays.asList(
            Migration.of(
                    1,
                    "create stats",
                    "CREATE TABLE stats (uuid VARCHAR(36) PRIMARY KEY, kills INT NOT NULL DEFAULT 0)"),
            Migration.of(
                    2,
                    "add rank",
                    tx -> tx.update(
                            "ALTER TABLE stats ADD COLUMN rank_name VARCHAR(16) NOT NULL " + "DEFAULT 'MEMBER'")));

    @BeforeEach
    void setUp() {
        db = Database.builder()
                .sqlite(dir.resolve("sub/data.db"))
                .poolName("Test")
                .build();
        MigrationResult result = db.migrate(MIGRATIONS).join();
        assertEquals(Arrays.asList(1, 2), result.applied());
        assertEquals(0, result.previousVersion());
        assertEquals(2, result.currentVersion());
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void crudWithTypedParameters() {
        UUID id = UUID.randomUUID();
        assertEquals(
                Integer.valueOf(1),
                db.update("INSERT INTO stats (uuid, kills, rank_name) VALUES (?, ?, ?)", id, 5, Rank.VIP)
                        .join());
        Optional<Integer> kills = db.queryOne("SELECT kills FROM stats WHERE uuid = ?", rs -> rs.getInt(1), id)
                .join();
        assertEquals(Integer.valueOf(5), kills.get());
        assertFalse(db.queryOne("SELECT kills FROM stats WHERE uuid = ?", rs -> rs.getInt(1), UUID.randomUUID())
                .join()
                .isPresent());

        db.update("CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, at BIGINT, flag BOOLEAN, note TEXT)")
                .join();
        Instant now = Instant.ofEpochMilli(1_700_000_000_123L);
        Optional<Long> key = db.insert("INSERT INTO events (at, flag, note) VALUES (?, ?, ?)", now, true, null)
                .join();
        assertEquals(Long.valueOf(1), key.get());
        assertEquals(
                Long.valueOf(1_700_000_000_123L),
                db.queryOne("SELECT at FROM events", rs -> rs.getLong(1)).join().get());

        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            rows.add(new Object[] {UUID.randomUUID(), i});
        }
        int[] counts =
                db.batch("INSERT INTO stats (uuid, kills) VALUES (?, ?)", rows).join();
        assertEquals(50, counts.length);
        assertEquals(
                Integer.valueOf(51),
                db.queryOne("SELECT COUNT(*) FROM stats", rs -> rs.getInt(1))
                        .join()
                        .get());
    }

    @Test
    void transactionsCommitAndRollBack() {
        final UUID a = UUID.randomUUID();
        int inserted = db.transaction(tx -> {
                    tx.update("INSERT INTO stats (uuid) VALUES (?)", a);
                    return tx.update("UPDATE stats SET kills = kills + 10 WHERE uuid = ?", a);
                })
                .join();
        assertEquals(1, inserted);

        final UUID b = UUID.randomUUID();
        CompletionException failure = assertThrows(
                CompletionException.class,
                () -> db.inTransaction(tx -> {
                            tx.update("INSERT INTO stats (uuid) VALUES (?)", b);
                            tx.update("INSERT INTO missing_table VALUES (1)");
                        })
                        .join());
        assertTrue(failure.getCause() instanceof StorageException);
        assertTrue(failure.getCause().getMessage().contains("missing_table"), "the SQL is named in the error");
        assertFalse(
                db.queryOne("SELECT 1 FROM stats WHERE uuid = ?", rs -> 1, b)
                        .join()
                        .isPresent(),
                "rolled back");

        AtomicReference<Transaction> leaked = new AtomicReference<>();
        db.inTransaction(leaked::set).join();
        assertThrows(IllegalStateException.class, () -> leaked.get().update("SELECT 1"));
    }

    @Test
    void migrationsAreIdempotentAndDetectDowngrades() {
        MigrationResult again = db.migrate(MIGRATIONS).join();
        assertTrue(again.applied().isEmpty());
        assertEquals(2, again.currentVersion());

        CompletionException downgrade = assertThrows(
                CompletionException.class,
                () -> db.migrate(Collections.singletonList(MIGRATIONS.get(0))).join());
        assertTrue(downgrade.getCause().getMessage().contains("newer than the newest migration"));

        List<Migration> broken = new ArrayList<>(MIGRATIONS);
        broken.add(Migration.of(3, "broken", "CREATE TABLE ok_table (id INT)", "THIS IS NOT SQL"));
        CompletionException failed =
                assertThrows(CompletionException.class, () -> db.migrate(broken).join());
        assertTrue(failed.getCause().getMessage().contains("Database migration 3 ('broken') failed"));
        assertEquals(
                Integer.valueOf(2),
                db.queryOne("SELECT MAX(version) FROM bloom_schema_history", rs -> rs.getInt(1))
                        .join()
                        .get());
        assertThrows(
                IllegalArgumentException.class, () -> db.migrate(Arrays.asList(MIGRATIONS.get(0), MIGRATIONS.get(0))));
    }

    @Test
    void repositoryUpsertFindDelete() {
        StatsRepository repository = new StatsRepository(db);
        UUID id = UUID.randomUUID();
        repository.save(new Stats(id, 1, Rank.MEMBER)).join();
        repository.save(new Stats(id, 7, Rank.VIP)).join(); // upsert
        Stats loaded = repository.find(id).join().get();
        assertEquals(7, loaded.kills);
        assertEquals(Rank.VIP, loaded.rank);
        assertEquals(1, repository.findAll().join().size());
        assertTrue(repository.delete(id).join());
        assertFalse(repository.delete(id).join());
        assertThrows(
                IllegalArgumentException.class,
                () -> new SqlRepository<UUID, Stats>(db, "bad name", "uuid", Arrays.asList("uuid"), rs -> null) {
                    @Override
                    protected UUID keyOf(Stats value) {
                        return null;
                    }

                    @Override
                    protected Object[] valuesOf(Stats value) {
                        return new Object[0];
                    }
                });
    }

    @Test
    void fullQueueFailsFastAndCloseCancelsQueuedWork() throws Exception {
        Database small = Database.builder()
                .sqlite(dir.resolve("small.db"))
                .queueCapacity(1)
                .shutdownTimeout(Duration.ofMillis(200))
                .build();
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        CompletableFuture<Object> blocker = small.withConnection(c -> {
            started.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        CompletableFuture<Integer> queued = small.update("CREATE TABLE t (id INT)");
        CompletableFuture<Integer> rejected = small.update("CREATE TABLE u (id INT)");
        CompletionException full = assertThrows(CompletionException.class, rejected::join);
        assertTrue(full.getCause().getMessage().contains("queue is full"));

        small.close(); // blocker still running -> queued work is cancelled after the timeout
        release.countDown();
        CompletionException cancelled = assertThrows(CompletionException.class, queued::join);
        assertTrue(cancelled.getCause().getMessage().contains("closed before the operation could run"));
        CompletionException closed = assertThrows(
                CompletionException.class, () -> small.update("SELECT 1").join());
        assertTrue(closed.getCause().getMessage().contains("closed"));
        blocker.join();
    }

    @Test
    void blockingApiRefusesTheMainThread() {
        Database guarded = Database.builder()
                .sqlite(dir.resolve("guarded.db"))
                .mainThreadCheck(() -> Thread.currentThread().getName().equals("main-thread"))
                .build();
        try {
            guarded.blocking().update("CREATE TABLE x (id INT)");
            assertEquals(1, guarded.blocking().update("INSERT INTO x VALUES (?)", 1));
            assertArrayEquals(
                    new int[] {1, 1},
                    guarded.blocking()
                            .batch("INSERT INTO x VALUES (?)", Arrays.asList(new Object[] {2}, new Object[] {3})));
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread main = new Thread(
                    () -> {
                        try {
                            guarded.blocking().query("SELECT * FROM x", rs -> rs.getInt(1));
                        } catch (Throwable t) {
                            error.set(t);
                        }
                    },
                    "main-thread");
            main.start();
            main.join();
            assertTrue(error.get() instanceof IllegalStateException);
            assertTrue(error.get().getMessage().contains("freeze the server"));
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        } finally {
            guarded.close();
        }
    }

    @Test
    void helpfulBuildErrors() {
        assertThrows(IllegalStateException.class, () -> Database.builder().build());
        StorageException unreachable = assertThrows(
                StorageException.class,
                () -> Database.builder()
                        .mariadb("127.0.0.1", 1, "x", "u", "p")
                        .connectionTimeout(Duration.ofSeconds(1))
                        .build());
        assertTrue(
                unreachable
                        .getMessage()
                        .startsWith("Could not connect to the MARIADB database at " + "jdbc:mariadb://127.0.0.1:1/x"),
                unreachable.getMessage());
        assertTrue(unreachable.getMessage().contains("Check the host, port"));
        StorageException missingDriver = assertThrows(
                StorageException.class,
                () -> Database.builder()
                        .jdbcUrl("jdbc:nothing:x", null, null, Dialect.MYSQL)
                        .driverClassName("com.example.MissingDriver")
                        .build());
        assertTrue(missingDriver.getMessage().contains("Could not connect"), missingDriver.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Database.builder().migrationTable("drop table; --"));
    }

    @Test
    void dialectUpsertSql() {
        List<String> cols = Arrays.asList("uuid", "kills", "rank_name");
        assertEquals(
                "INSERT OR REPLACE INTO stats (uuid, kills, rank_name) VALUES (?, ?, ?)",
                Dialect.SQLITE.upsert("stats", cols, Collections.singletonList("uuid")));
        assertEquals(
                "INSERT INTO stats (uuid, kills, rank_name) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE "
                        + "kills = VALUES(kills), rank_name = VALUES(rank_name)",
                Dialect.MYSQL.upsert("stats", cols, Collections.singletonList("uuid")));
    }
}
