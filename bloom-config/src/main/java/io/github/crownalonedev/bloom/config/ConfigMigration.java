package io.github.crownalonedev.bloom.config;

/**
 * Upgrades a configuration from one version to the next. Registered with
 * {@link YamlConfig.Builder#migration(int, ConfigMigration)}.
 *
 * <pre>{@code
 * YamlConfig config = YamlConfig.builder(file)
 *         .defaults(() -> plugin.getResource("config.yml"))
 *         .version(3)
 *         .migration(1, m -> m.move("prefix", "messages.prefix"))            // v1 -> v2
 *         .migration(2, m -> m.set("storage.type", m.has("mysql") ? "mysql" : "sqlite")) // v2 -> v3
 *         .build();
 * }</pre>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface ConfigMigration {

    /**
     * Applies the migration to the configuration values.
     *
     * @param context mutable view of the values being migrated
     */
    void migrate(MigrationContext context);
}
