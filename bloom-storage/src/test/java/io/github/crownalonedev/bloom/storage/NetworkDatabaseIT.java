package io.github.crownalonedev.bloom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

/**
 * Verifies MySQL and MariaDB against real servers. Run with {@code scripts/test-network-databases.sh}, which starts
 * containers and sets {@code BLOOM_TEST_MYSQL_PORT} / {@code BLOOM_TEST_MARIADB_PORT}. Excluded from normal builds.
 */
class NetworkDatabaseIT {

    private static void exercise(Database db) {
        try {
            db.update("DROP TABLE IF EXISTS bloom_it_stats").join();
            db.update("DROP TABLE IF EXISTS bloom_schema_history").join();
            MigrationResult result = db.migrate(Arrays.asList(
                            Migration.of(
                                    1,
                                    "create",
                                    "CREATE TABLE bloom_it_stats (uuid VARCHAR(36) PRIMARY KEY, "
                                            + "kills INT NOT NULL DEFAULT 0)"),
                            Migration.of(
                                    2,
                                    "column",
                                    "ALTER TABLE bloom_it_stats ADD COLUMN rank_name VARCHAR(16) "
                                            + "NOT NULL DEFAULT 'MEMBER'")))
                    .join();
            assertEquals(2, result.currentVersion());

            UUID id = UUID.randomUUID();
            String upsert = db.dialect()
                    .upsert("bloom_it_stats", Arrays.asList("uuid", "kills", "rank_name"), Arrays.asList("uuid"));
            db.update(upsert, id, 1, "MEMBER").join();
            db.update(upsert, id, 9, "VIP").join();
            assertEquals(
                    Integer.valueOf(9),
                    db.queryOne("SELECT kills FROM bloom_it_stats WHERE uuid = ?", rs -> rs.getInt(1), id)
                            .join()
                            .get());

            UUID rolledBack = UUID.randomUUID();
            CompletionException failure = null;
            try {
                db.inTransaction(tx -> {
                            tx.update("INSERT INTO bloom_it_stats (uuid) VALUES (?)", rolledBack);
                            tx.update("INSERT INTO no_such_table VALUES (1)");
                        })
                        .join();
            } catch (CompletionException e) {
                failure = e;
            }
            assertTrue(failure != null && failure.getCause() instanceof StorageException);
            assertFalse(db.queryOne("SELECT 1 FROM bloom_it_stats WHERE uuid = ?", rs -> 1, rolledBack)
                    .join()
                    .isPresent());

            assertTrue(
                    db.migrate(Arrays.asList(
                                    Migration.of(1, "create", "SELECT 1"), Migration.of(2, "column", "SELECT 1")))
                            .join()
                            .applied()
                            .isEmpty(),
                    "already applied migrations are skipped");
        } finally {
            db.update("DROP TABLE IF EXISTS bloom_it_stats").join();
            db.close();
        }
    }

    @Test
    void mysql() {
        String port = System.getenv("BLOOM_TEST_MYSQL_PORT");
        assumeTrue(port != null && !port.isEmpty(), "BLOOM_TEST_MYSQL_PORT not set");
        exercise(Database.builder()
                .mysql("127.0.0.1", Integer.parseInt(port), "bloom", "bloom", "bloom")
                .property("allowPublicKeyRetrieval", "true")
                .property("useSSL", "false")
                .build());
    }

    @Test
    void mariadb() {
        String port = System.getenv("BLOOM_TEST_MARIADB_PORT");
        assumeTrue(port != null && !port.isEmpty(), "BLOOM_TEST_MARIADB_PORT not set");
        exercise(Database.builder()
                .mariadb("127.0.0.1", Integer.parseInt(port), "bloom", "bloom", "bloom")
                .build());
    }
}
