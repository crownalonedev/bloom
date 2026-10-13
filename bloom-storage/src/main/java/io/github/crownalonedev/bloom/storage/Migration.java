package io.github.crownalonedev.bloom.storage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A numbered schema change, applied once per database by {@link Database#migrate(List)}.
 *
 * <pre>{@code
 * List<Migration> migrations = Arrays.asList(
 *         Migration.of(1, "create stats",
 *                 "CREATE TABLE IF NOT EXISTS stats (uuid VARCHAR(36) PRIMARY KEY, kills INT NOT NULL DEFAULT 0)"),
 *         Migration.of(2, "add deaths", tx -> tx.update("ALTER TABLE stats ADD COLUMN deaths INT NOT NULL DEFAULT 0")));
 * database.migrate(migrations).join(); // blocking is acceptable in onEnable, before players join
 * }</pre>
 *
 * <p>Each migration runs in its own transaction. Note that MySQL/MariaDB commit DDL statements immediately, so a
 * failing multi-statement migration can leave earlier statements applied there; keep migrations small.
 *
 * @since 0.1.0
 */
public final class Migration {

    /** Migration body. */
    @FunctionalInterface
    public interface Step {
        /**
         * Applies the change.
         *
         * @param transaction transaction to execute statements in
         */
        void apply(Transaction transaction);
    }

    private final int version;
    private final String description;
    private final Step step;

    private Migration(int version, String description, Step step) {
        if (version < 1) {
            throw new IllegalArgumentException("Migration versions start at 1, got " + version);
        }
        this.version = version;
        this.description = Objects.requireNonNull(description, "description");
        this.step = Objects.requireNonNull(step, "step");
    }

    /**
     * Creates a migration from SQL statements.
     *
     * @param version unique positive version
     * @param description short description stored in the history table
     * @param statements statements to execute in order
     * @return migration
     */
    public static Migration of(int version, String description, String... statements) {
        final List<String> sql = Collections.unmodifiableList(Arrays.asList(statements.clone()));
        return new Migration(version, description, tx -> {
            for (String statement : sql) {
                tx.update(statement);
            }
        });
    }

    /**
     * Creates a migration with custom logic (e.g. dialect-specific SQL or data transformation).
     *
     * @param version unique positive version
     * @param description short description
     * @param step migration body
     * @return migration
     */
    public static Migration of(int version, String description, Step step) {
        return new Migration(version, description, step);
    }

    /**
     * Returns the version.
     *
     * @return version
     */
    public int version() {
        return version;
    }

    /**
     * Returns the description.
     *
     * @return description
     */
    public String description() {
        return description;
    }

    Step step() {
        return step;
    }
}
