package io.github.crownalonedev.bloom.storage;

import java.util.Collections;
import java.util.List;

/**
 * Outcome of {@link Database#migrate(List)}.
 *
 * @since 0.1.0
 */
public final class MigrationResult {

    private final int previousVersion;
    private final int currentVersion;
    private final List<Integer> applied;

    MigrationResult(int previousVersion, int currentVersion, List<Integer> applied) {
        this.previousVersion = previousVersion;
        this.currentVersion = currentVersion;
        this.applied = Collections.unmodifiableList(applied);
    }

    /**
     * Returns the schema version before migrating.
     *
     * @return version, {@code 0} for a new database
     */
    public int previousVersion() {
        return previousVersion;
    }

    /**
     * Returns the schema version after migrating.
     *
     * @return version
     */
    public int currentVersion() {
        return currentVersion;
    }

    /**
     * Returns the versions applied by this run.
     *
     * @return applied versions in order
     */
    public List<Integer> applied() {
        return applied;
    }

    @Override
    public String toString() {
        return "MigrationResult[" + previousVersion + " -> " + currentVersion + ", applied=" + applied + "]";
    }
}
