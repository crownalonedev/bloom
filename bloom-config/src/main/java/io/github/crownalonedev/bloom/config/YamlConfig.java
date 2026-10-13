package io.github.crownalonedev.bloom.config;

import io.github.crownalonedev.bloom.config.internal.ConfigPaths;
import io.github.crownalonedev.bloom.config.internal.ObjectMapper;
import io.github.crownalonedev.bloom.config.internal.YamlIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * A YAML configuration file with bundled defaults, typed access, validation, migrations and safe writes.
 *
 * <pre>{@code
 * YamlConfig config = YamlConfig.builder(getDataFolder().toPath().resolve("config.yml"))
 *         .defaults(() -> getResource("config.yml"))
 *         .logger(getLogger())
 *         .build();                       // creates the file from defaults if missing, then loads it
 *
 * String prefix = config.getString("messages.prefix");
 * int maxHomes  = config.getInt("homes.max", 3);
 * Settings s    = config.get("settings", Settings.class);   // POJO mapping
 * }</pre>
 *
 * <h2>Safety</h2>
 *
 * <ul>
 *   <li>A missing file is created by copying the defaults resource byte-for-byte (comments included).
 *   <li>Existing files are never rewritten implicitly, except by migrations (which first copy the file to a
 *       timestamped {@code .bak}) and by {@link Builder#addMissingDefaults(boolean)} (opt-in, also backed up).
 *   <li>{@link #save()} and migrations edit the file's YAML node tree, so comments and formatting of untouched keys
 *       are preserved. Writes are atomic (temporary file + move).
 *   <li>{@link #reload()} keeps the previous values if the new file is invalid.
 *   <li>A file whose version is newer than the plugin supports is rejected rather than downgraded.
 * </ul>
 *
 * <p>Values are loaded with SnakeYAML's {@code SafeConstructor} (no arbitrary object construction), duplicate keys are
 * rejected and alias expansion is limited. Thread-safe.
 *
 * @since 0.1.0
 */
public final class YamlConfig extends ConfigSection {

    private final Path file;
    private final Supplier<InputStream> defaultsSource;
    private final String versionKey;
    private final int version;
    private final Map<Integer, ConfigMigration> migrations;
    private final boolean addMissingDefaults;
    private final Logger logger;
    private final ObjectMapper mapper;

    private String defaultsText;
    private Map<String, Object> defaults = new LinkedHashMap<String, Object>();
    private String fileText;
    private Map<String, Object> values = new LinkedHashMap<String, Object>();
    private Map<String, Integer> lines = Collections.emptyMap();

    private YamlConfig(Builder builder) {
        super(null, "");
        this.file = builder.file;
        this.defaultsSource = builder.defaults;
        this.versionKey = builder.versionKey;
        this.version = builder.version;
        this.migrations = new TreeMap<Integer, ConfigMigration>(builder.migrations);
        this.addMissingDefaults = builder.addMissingDefaults;
        this.logger = builder.logger;
        this.mapper = builder.mapper;
    }

    /**
     * Starts configuring a YAML file.
     *
     * @param file path of the config file
     * @return builder
     */
    public static Builder builder(Path file) {
        return new Builder(file);
    }

    /**
     * Returns the file.
     *
     * @return path
     */
    public Path file() {
        return file;
    }

    /**
     * Returns the configured target version, or {@code 0} when versioning is not used.
     *
     * @return version
     */
    public int version() {
        return version;
    }

    /**
     * Re-reads the file. On failure the previous values stay active and the exception is thrown.
     *
     * @throws ConfigException if the file is invalid or a migration fails
     */
    public synchronized void reload() {
        load();
    }

    /**
     * Writes the current values to the file, preserving comments and formatting of unchanged keys. Atomic.
     *
     * @throws ConfigException on I/O failure
     */
    public synchronized void save() {
        Map<String, Object> current = fileText == null ? null : YamlIO.parse(fileText, name());
        String rendered = YamlIO.render(fileText, current, values, defaultsText);
        write(rendered);
        fileText = rendered;
        lines = YamlIO.lines(YamlIO.compose(rendered));
    }

    /**
     * Starts a validation of the current values.
     *
     * @return validation builder; call {@link ConfigValidation#check()} to throw on problems
     */
    public ConfigValidation validate() {
        return new ConfigValidation(this);
    }

    // ---- loading ------------------------------------------------------------------------------------------

    private void load() {
        String loadedDefaultsText = readDefaults();
        Map<String, Object> loadedDefaults = loadedDefaultsText == null
                ? new LinkedHashMap<String, Object>()
                : YamlIO.parse(loadedDefaultsText, name() + " (bundled defaults)");

        String text;
        try {
            text = YamlIO.read(file);
        } catch (NoSuchFileException e) {
            text = null;
        } catch (IOException e) {
            throw new ConfigException("Could not read " + file + ": " + e.getMessage(), e);
        }
        if (text == null) {
            text = loadedDefaultsText == null ? "" : loadedDefaultsText;
            write(text);
            if (logger != null && loadedDefaultsText != null) {
                logger.info("Created " + file.getFileName() + " from defaults.");
            }
        }
        Map<String, Object> loaded = YamlIO.parse(text, name());
        String finalText = text;

        if (version > 0) {
            int fileVersion = readVersion(loaded);
            if (fileVersion > version) {
                throw new ConfigException(name() + " has " + versionKey + " " + fileVersion + ", but this plugin "
                        + "only understands up to " + version + ". It was probably written by a newer plugin "
                        + "version; refusing to load it to avoid data loss.");
            }
            if (fileVersion < version) {
                Map<String, Object> migrated = migrate(loaded, fileVersion);
                Path backup = backup("v" + fileVersion);
                finalText = YamlIO.render(text, loaded, migrated, loadedDefaultsText);
                write(finalText);
                loaded = migrated;
                if (logger != null) {
                    logger.info("Migrated " + file.getFileName() + " from version " + fileVersion + " to " + version
                            + " (backup: " + backup.getFileName() + ").");
                }
            }
        }
        if (addMissingDefaults) {
            Map<String, Object> completed = deepCopyMap(loaded);
            int added = ConfigPaths.addMissing(completed, loadedDefaults);
            if (added > 0) {
                Path backup = backup("pre-defaults");
                finalText = YamlIO.render(finalText, loaded, completed, loadedDefaultsText);
                write(finalText);
                loaded = completed;
                if (logger != null) {
                    logger.info("Added " + added + " new default setting(s) to " + file.getFileName() + " (backup: "
                            + backup.getFileName() + ").");
                }
            }
        }
        // Commit only after everything succeeded.
        this.defaultsText = loadedDefaultsText;
        this.defaults = loadedDefaults;
        this.fileText = finalText;
        this.values = loaded;
        this.lines = YamlIO.lines(YamlIO.compose(finalText));
    }

    private Map<String, Object> migrate(Map<String, Object> loaded, int fileVersion) {
        Map<String, Object> working = deepCopyMap(loaded);
        for (int from = fileVersion; from < version; from++) {
            ConfigMigration migration = migrations.get(from);
            if (migration == null) {
                throw new ConfigException("No migration registered from " + versionKey + " " + from + " to "
                        + (from + 1) + " for " + name() + ". Register one with .migration(" + from + ", ...).");
            }
            try {
                migration.migrate(new MigrationContext(working, from));
            } catch (RuntimeException e) {
                throw new ConfigException(
                        "Migration of " + name() + " from version " + from + " failed; the file " + "was not changed.",
                        e);
            }
        }
        working.put(versionKey, version);
        return working;
    }

    private int readVersion(Map<String, Object> loaded) {
        Object raw = loaded.get(versionKey);
        if (raw == null) {
            return 1;
        }
        try {
            return (Integer) mapper.deserialize(raw, Integer.class);
        } catch (IllegalArgumentException e) {
            throw error(versionKey, e.getMessage());
        }
    }

    private String readDefaults() {
        if (defaultsSource == null) {
            return null;
        }
        InputStream in = defaultsSource.get();
        if (in == null) {
            throw new ConfigException("The defaults resource for " + name() + " was not found. Is it included in the "
                    + "plugin jar (src/main/resources)?");
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigException("Could not read defaults for " + name() + ": " + e.getMessage(), e);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // nothing useful to do
            }
        }
    }

    private Path backup(String label) {
        if (!Files.exists(file)) {
            return file;
        }
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Path backup = file.resolveSibling(file.getFileName() + "." + label + "." + stamp + ".bak");
        try {
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ConfigException(
                    "Could not back up " + file + " before modifying it; the file was not changed: " + e.getMessage(),
                    e);
        }
        return backup;
    }

    private void write(String text) {
        try {
            YamlIO.writeAtomically(file, text);
        } catch (IOException e) {
            throw new ConfigException("Could not write " + file + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepCopyMap(Map<String, Object> map) {
        return (Map<String, Object>) ConfigPaths.deepCopy(map);
    }

    // ---- access used by ConfigSection ---------------------------------------------------------------------

    @Override
    String absolute(String path) {
        return path;
    }

    synchronized Object raw(String path) {
        if (path.isEmpty()) {
            return mergedRoot();
        }
        Object value = ConfigPaths.get(values, path);
        return value != null ? value : ConfigPaths.get(defaults, path);
    }

    synchronized Object rawFromFile(String path) {
        return ConfigPaths.get(values, path);
    }

    synchronized Object rawFromDefaults(String path) {
        return ConfigPaths.get(defaults, path);
    }

    synchronized Map<String, Object> fileValues() {
        return values;
    }

    synchronized Map<String, Object> defaultValues() {
        return defaults;
    }

    /** File values completed with defaults, as a copy. */
    synchronized Map<String, Object> mergedRoot() {
        Map<String, Object> merged = deepCopyMap(values);
        ConfigPaths.addMissing(merged, defaults);
        return merged;
    }

    synchronized void setValue(String path, Object value) {
        ConfigPaths.set(values, path, mapper.serialize(value));
    }

    ObjectMapper mapper() {
        return mapper;
    }

    String name() {
        return file.getFileName().toString();
    }

    synchronized String locate(String path) {
        Integer line = lines.get(path);
        if (line != null) {
            return name() + " line " + line + ": '" + path + "'";
        }
        if (ConfigPaths.get(values, path) == null && ConfigPaths.get(defaults, path) != null) {
            return name() + " (default value): '" + path + "'";
        }
        return name() + ": '" + path + "'";
    }

    ConfigException error(String path, String problem) {
        return new ConfigException(locate(path) + " " + problem);
    }

    @Override
    public String toString() {
        return "YamlConfig[" + file + "]";
    }

    /** Builder for {@link YamlConfig}. */
    public static final class Builder {
        private final Path file;
        private Supplier<InputStream> defaults;
        private String versionKey = "config-version";
        private int version;
        private final Map<Integer, ConfigMigration> migrations = new TreeMap<Integer, ConfigMigration>();
        private boolean addMissingDefaults;
        private Logger logger;
        private final ObjectMapper mapper = new ObjectMapper();

        private Builder(Path file) {
            this.file = Objects.requireNonNull(file, "file");
        }

        /**
         * Sets the bundled defaults, usually {@code () -> plugin.getResource("config.yml")}. The stream is read once
         * per load and closed.
         *
         * @param source supplies the defaults YAML; returning {@code null} is reported as a missing resource
         * @return this builder
         */
        public Builder defaults(Supplier<InputStream> source) {
            this.defaults = Objects.requireNonNull(source, "source");
            return this;
        }

        /**
         * Enables versioned migrations with the key {@code config-version}.
         *
         * @param currentVersion version this plugin writes, {@code >= 1}
         * @return this builder
         */
        public Builder version(int currentVersion) {
            return version("config-version", currentVersion);
        }

        /**
         * Enables versioned migrations with a custom key. Files without the key are treated as version 1.
         *
         * @param key version key at the top level
         * @param currentVersion version this plugin writes, {@code >= 1}
         * @return this builder
         */
        public Builder version(String key, int currentVersion) {
            if (currentVersion < 1) {
                throw new IllegalArgumentException("Config version must be >= 1");
            }
            this.versionKey = Objects.requireNonNull(key, "key");
            this.version = currentVersion;
            return this;
        }

        /**
         * Registers the migration from {@code fromVersion} to {@code fromVersion + 1}.
         *
         * @param fromVersion source version
         * @param migration migration
         * @return this builder
         */
        public Builder migration(int fromVersion, ConfigMigration migration) {
            migrations.put(fromVersion, Objects.requireNonNull(migration, "migration"));
            return this;
        }

        /**
         * Writes settings that exist in the defaults but not in the file into the file on load (with their default
         * comments). The file is backed up first. Off by default.
         *
         * @param enabled flag
         * @return this builder
         */
        public Builder addMissingDefaults(boolean enabled) {
            this.addMissingDefaults = enabled;
            return this;
        }

        /**
         * Sets a logger for informational messages (file created, migrated).
         *
         * @param target logger, usually {@code plugin.getLogger()}
         * @return this builder
         */
        public Builder logger(Logger target) {
            this.logger = target;
            return this;
        }

        /**
         * Registers a serializer for a custom type.
         *
         * @param type type
         * @param serializer serializer
         * @param <T> type
         * @return this builder
         */
        public <T> Builder serializer(Class<T> type, TypeSerializer<T> serializer) {
            mapper.register(type, serializer);
            return this;
        }

        /**
         * Creates the config and loads it (creating the file from defaults if needed and running migrations).
         *
         * @return the loaded config
         * @throws ConfigException if loading fails
         */
        public YamlConfig build() {
            YamlConfig config = new YamlConfig(this);
            synchronized (config) {
                config.load();
            }
            return config;
        }
    }
}
