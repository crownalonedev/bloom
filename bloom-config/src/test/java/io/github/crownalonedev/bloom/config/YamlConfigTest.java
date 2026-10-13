package io.github.crownalonedev.bloom.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlConfigTest {

    @TempDir
    Path dir;

    private static final String DEFAULTS = "# Bloom test config\n"
            + "config-version: 1\n"
            + "\n"
            + "# Messages shown to players\n"
            + "messages:\n"
            + "  prefix: '&d[Bloom] '  # chat prefix\n"
            + "  welcome: Welcome!\n"
            + "\n"
            + "homes:\n"
            + "  max: 3\n"
            + "  cooldown: 30s\n"
            + "mode: survival\n"
            + "worlds:\n"
            + "  - world\n"
            + "  - world_nether\n";

    private static Supplier<InputStream> resource(final String text) {
        return () -> new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private Path file() {
        return dir.resolve("config.yml");
    }

    private void write(String text) throws IOException {
        Files.write(file(), text.getBytes(StandardCharsets.UTF_8));
    }

    private String read() throws IOException {
        return new String(Files.readAllBytes(file()), StandardCharsets.UTF_8);
    }

    private List<Path> backups() throws IOException {
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.bak")) {
            for (Path p : stream) {
                out.add(p);
            }
        }
        return out;
    }

    enum Mode {
        SURVIVAL,
        CREATIVE
    }

    static final class Homes {
        int max = 1;
        Duration cooldown = Duration.ZERO;
        boolean allowNether = true;
    }

    static final class Settings {
        Homes homes = new Homes();
        Mode mode = Mode.CREATIVE;
        List<String> worlds = new ArrayList<>();

        @ConfigKey("messages")
        java.util.Map<String, String> messageMap;
    }

    @Test
    void createsFileFromDefaultsVerbatim() throws IOException {
        YamlConfig config =
                YamlConfig.builder(file()).defaults(resource(DEFAULTS)).build();
        assertEquals(DEFAULTS, read(), "defaults are copied byte-for-byte, comments included");
        assertEquals("&d[Bloom] ", config.getString("messages.prefix"));
        assertEquals(3, config.getInt("homes.max"));
        assertEquals(Duration.ofSeconds(30), config.getDuration("homes.cooldown"));
        assertEquals(Arrays.asList("world", "world_nether"), config.getStringList("worlds"));
        assertEquals(Mode.SURVIVAL, config.getEnum("mode", Mode.class));
    }

    @Test
    void missingValuesFallBackToDefaultsAndFallbacks() throws IOException {
        write("homes:\n  max: 7\n");
        YamlConfig config =
                YamlConfig.builder(file()).defaults(resource(DEFAULTS)).build();
        assertEquals(7, config.getInt("homes.max"));
        assertEquals(Duration.ofSeconds(30), config.getDuration("homes.cooldown"), "leaf falls back to default");
        assertEquals("Welcome!", config.getString("messages.welcome"));
        assertEquals(42, config.getInt("not.there", 42));
        ConfigException missing = assertThrows(ConfigException.class, () -> config.getString("not.there"));
        assertTrue(missing.getMessage().contains("'not.there' is missing"), missing.getMessage());
        assertTrue(config.isSet("homes.max"));
        assertFalse(config.isSet("homes.cooldown"));
        assertTrue(config.has("homes.cooldown"));
        assertEquals(Collections.singleton("max"), config.section("homes").keys(), "file sections are not refilled");
    }

    @Test
    void typeErrorsNameFileLineAndPath() throws IOException {
        write("homes:\n  max: ten\n");
        YamlConfig config = YamlConfig.builder(file()).build();
        ConfigException e = assertThrows(ConfigException.class, () -> config.getInt("homes.max"));
        assertEquals("config.yml line 2: 'homes.max' expected a whole number but found 'ten'", e.getMessage());
    }

    @Test
    void invalidYamlReportsLine() throws IOException {
        write("a: 1\nb: [unclosed\nc: 3\n");
        ConfigException e = assertThrows(
                ConfigException.class, () -> YamlConfig.builder(file()).build());
        assertTrue(e.getMessage().startsWith("Invalid YAML in config.yml at line"), e.getMessage());
    }

    @Test
    void duplicateKeysAreRejected() throws IOException {
        write("a: 1\na: 2\n");
        assertThrows(ConfigException.class, () -> YamlConfig.builder(file()).build());
    }

    @Test
    void savePreservesCommentsAndFormatting() throws IOException {
        YamlConfig config =
                YamlConfig.builder(file()).defaults(resource(DEFAULTS)).build();
        config.set("homes.max", 10);
        config.set("new-section.enabled", true);
        config.save();
        String saved = read();
        assertTrue(saved.contains("# Bloom test config"), saved);
        assertTrue(saved.contains("# Messages shown to players"), saved);
        assertTrue(saved.contains("# chat prefix"), saved);
        assertTrue(saved.contains("  max: 10"), saved);
        assertTrue(saved.contains("new-section:\n  enabled: true"), saved);
        assertTrue(saved.contains("prefix: '&d[Bloom] '"), "untouched values keep their quoting: " + saved);

        YamlConfig reloaded = YamlConfig.builder(file()).build();
        assertEquals(10, reloaded.getInt("homes.max"));
        assertTrue(reloaded.getBoolean("new-section.enabled"));
    }

    @Test
    void reloadKeepsPreviousValuesWhenFileIsBroken() throws IOException {
        YamlConfig config =
                YamlConfig.builder(file()).defaults(resource(DEFAULTS)).build();
        write("homes: [broken\n");
        assertThrows(ConfigException.class, config::reload);
        assertEquals(3, config.getInt("homes.max"));
        write("homes:\n  max: 5\n");
        config.reload();
        assertEquals(5, config.getInt("homes.max"));
    }

    @Test
    void migrationsUpgradeWithBackupAndKeepComments() throws IOException {
        write("# my server\nprefix: '&c>> '  # keep me\nhomes:\n  max: 9\n");
        YamlConfig config = YamlConfig.builder(file())
                .version(3)
                .migration(1, m -> m.move("prefix", "messages.prefix"))
                .migration(2, m -> m.set("homes.max", ((Integer) m.get("homes.max")) * 2))
                .build();
        assertEquals("&c>> ", config.getString("messages.prefix"));
        assertEquals(18, config.getInt("homes.max"));
        assertEquals(3, config.getInt("config-version"));
        String saved = read();
        assertTrue(saved.contains("# my server"), saved);
        assertTrue(saved.contains("config-version: 3"), saved);
        assertEquals(1, backups().size());
        String backup = new String(Files.readAllBytes(backups().get(0)), StandardCharsets.UTF_8);
        assertTrue(backup.contains("prefix: '&c>> '"));

        // Already migrated: loading again does nothing.
        YamlConfig.builder(file()).version(3).build();
        assertEquals(1, backups().size());
    }

    @Test
    void migrationProblemsAreReportedWithoutTouchingTheFile() throws IOException {
        write("config-version: 1\na: 1\n");
        ConfigException missing = assertThrows(
                ConfigException.class,
                () -> YamlConfig.builder(file()).version(2).build());
        assertTrue(missing.getMessage().contains("No migration registered from config-version 1 to 2"));

        ConfigException failing = assertThrows(
                ConfigException.class,
                () -> YamlConfig.builder(file())
                        .version(2)
                        .migration(1, m -> {
                            throw new IllegalStateException("boom");
                        })
                        .build());
        assertTrue(failing.getMessage().contains("was not changed"));
        assertEquals("config-version: 1\na: 1\n", read());
        assertTrue(backups().isEmpty());

        write("config-version: 5\n");
        ConfigException newer = assertThrows(
                ConfigException.class,
                () -> YamlConfig.builder(file()).version(2).build());
        assertTrue(newer.getMessage().contains("refusing to load"));
    }

    @Test
    void addMissingDefaultsWritesNewKeysWithComments() throws IOException {
        write("homes:\n  max: 9\n");
        YamlConfig config = YamlConfig.builder(file())
                .defaults(resource(DEFAULTS))
                .addMissingDefaults(true)
                .build();
        String saved = read();
        assertTrue(saved.contains("max: 9"), saved);
        assertTrue(saved.contains("cooldown: 30s"), saved);
        assertTrue(saved.contains("# Messages shown to players"), "new keys get their default comments: " + saved);
        assertEquals(9, config.getInt("homes.max"));
        assertEquals(1, backups().size());
    }

    @Test
    void objectMappingWithDefaultsAndKebabCase() throws IOException {
        write("homes:\n  max: 5\n  allow-nether: false\nmode: creative\nworlds: [a, b]\nmessages:\n  hi: hello\n");
        YamlConfig config = YamlConfig.builder(file()).build();
        Settings settings = config.as(Settings.class);
        assertEquals(5, settings.homes.max);
        assertFalse(settings.homes.allowNether);
        assertEquals(Duration.ZERO, settings.homes.cooldown, "missing keys keep field defaults");
        assertEquals(Mode.CREATIVE, settings.mode);
        assertEquals(Arrays.asList("a", "b"), settings.worlds);
        assertEquals("hello", settings.messageMap.get("hi"));

        settings.homes.cooldown = Duration.ofMillis(1500);
        config.set("homes", settings.homes);
        config.save();
        Homes reloaded = YamlConfig.builder(file()).build().get("homes", Homes.class);
        assertEquals(Duration.ofMillis(1500), reloaded.cooldown);
    }

    @Test
    void mappingErrorsIncludePathOfNestedField() throws IOException {
        write("homes:\n  max: lots\n");
        YamlConfig config = YamlConfig.builder(file()).build();
        ConfigException e = assertThrows(ConfigException.class, () -> config.get("homes", Homes.class));
        assertTrue(e.getMessage().contains("'max': expected a whole number"), e.getMessage());
    }

    @Test
    void customSerializers() throws IOException {
        write("spawn: 'world,1,64,-3'\n");
        YamlConfig config = YamlConfig.builder(file())
                .serializer(int[].class, new TypeSerializer<int[]>() {
                    @Override
                    public int[] deserialize(Object raw) {
                        String[] parts = raw.toString().split(",");
                        return new int[] {
                            Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])
                        };
                    }

                    @Override
                    public Object serialize(int[] value) {
                        return "world," + value[0] + "," + value[1] + "," + value[2];
                    }
                })
                .build();
        int[] spawn = config.get("spawn", int[].class);
        assertEquals(-3, spawn[2]);
        config.set("spawn", new int[] {0, 70, 0});
        assertEquals("world,0,70,0", config.getString("spawn"));
    }

    @Test
    void validationReportsAllProblems() throws IOException {
        write("database:\n  type: postgres\nlimits:\n  homes: 500\nname: 'Bad Name'\n");
        YamlConfig config = YamlConfig.builder(file()).build();
        ConfigException e = assertThrows(
                ConfigException.class,
                () -> config.validate()
                        .require("database.type")
                        .require("database.host")
                        .oneOf("database.type", "sqlite", "mysql")
                        .range("limits.homes", 1, 100)
                        .matches("name", java.util.regex.Pattern.compile("[a-z_]+"), "lowercase letters")
                        .check("limits.homes", Integer.class, v -> v % 2 == 1, "must be odd")
                        .check());
        assertEquals(5, e.problems().size(), e.getMessage());
        assertTrue(e.getMessage().contains("config.yml line 2: 'database.type' must be one of [sqlite, mysql]"));
        assertTrue(e.getMessage().contains("config.yml line 4: 'limits.homes' must be between 1 and 100, but is 500"));
    }

    @Test
    void missingDefaultsResourceIsExplained() {
        ConfigException e = assertThrows(
                ConfigException.class,
                () -> YamlConfig.builder(file()).defaults(() -> null).build());
        assertTrue(e.getMessage().contains("src/main/resources"));
    }
}
