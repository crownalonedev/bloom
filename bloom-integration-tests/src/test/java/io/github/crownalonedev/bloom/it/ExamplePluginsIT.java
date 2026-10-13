package io.github.crownalonedev.bloom.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Loads the shaded, relocated example plugin jars (compiled against spigot-api 1.8.8 as Java 8 bytecode) into
 * MockBukkit's Paper 1.21 server. Verifies that relocation and reflective adapter loading work in shipped artifacts.
 */
class ExamplePluginsIT {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Plugin load(String example) {
        String dir = System.getProperty("bloom.examples.dir");
        String version = System.getProperty("bloom.version");
        File jar = new File(dir, "example-" + example + "/build/libs/example-" + example + "-" + version + ".jar");
        assertTrue(jar.isFile(), "missing " + jar);
        Plugin plugin = MockBukkit.loadJar(jar);
        if (!plugin.isEnabled()) {
            server.getPluginManager().enablePlugin(plugin); // loadJar loads without enabling
        }
        assertTrue(plugin.isEnabled(), example + " should enable");
        return plugin;
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void basicExampleRunsCommandsAndShutsDownCleanly() {
        Plugin plugin = load("basic");
        PlayerMock player = server.addPlayer();
        String welcome = plain(player.nextComponentMessage());
        assertTrue(welcome.startsWith("Bloom » Welcome, " + player.getName()), welcome);
        assertTrue(player.performCommand("hello"));
        assertEquals("Bloom » Hello, " + player.getName() + "!", plain(player.nextComponentMessage()));
        assertNotNull(player.nextActionBar());
        assertTrue(player.performCommand("hello"));
        assertTrue(plain(player.nextComponentMessage()).contains("Slow down"), "cooldown applies");
        server.getPluginManager().disablePlugin(plugin);
        assertFalse(plugin.isEnabled());
    }

    @Test
    void menusExampleOpensRelocatedMenu() {
        load("menus");
        PlayerMock player = server.addPlayer();
        assertTrue(player.performCommand("shop"));
        assertEquals(27, player.getOpenInventory().getTopInventory().getSize());
        assertEquals("Blossom Shop", plain(player.getOpenInventory().title()));
        assertTrue(
                player.simulateInventoryClick(player.getOpenInventory(), org.bukkit.event.inventory.ClickType.LEFT, 11)
                        .isCancelled());
    }

    @Test
    void itemsExampleCreatesTaggedItems() {
        load("items");
        PlayerMock player = server.addPlayer();
        assertTrue(player.performCommand("bloomitems"));
        assertEquals(
                org.bukkit.Material.RED_DYE, player.getInventory().getItem(0).getType());
        assertEquals(3, player.getInventory().getItem(0).getAmount());
        assertTrue(player.getInventory().getItem(0).getItemMeta().getPersistentDataContainer().getKeys().stream()
                .anyMatch(key -> key.toString().equals("bloomexampleitems:item_id")));
    }

    @Test
    void configExampleCreatesFileFromDefaults() throws Exception {
        Plugin plugin = load("config");
        File config = new File(plugin.getDataFolder(), "config.yml");
        assertTrue(config.isFile());
        assertTrue(new String(Files.readAllBytes(config.toPath()), "UTF-8").contains("# Maximum homes per player"));
        PlayerMock op = server.addPlayer();
        op.setOp(true);
        assertTrue(op.performCommand("bloomconfig"));
        assertTrue(plain(op.nextComponentMessage()).contains("Max homes: 3"));
    }

    @Test
    void storageExampleMigratesSqlite() {
        Plugin plugin = load("storage");
        assertTrue(new File(plugin.getDataFolder(), "stats.db").isFile());
        server.getPluginManager().disablePlugin(plugin); // closes the database through Bloom
    }

    @Test
    void crossVersionExampleReportsCapabilities() {
        load("cross-version");
        PlayerMock player = server.addPlayer();
        assertTrue(player.performCommand("bloominfo"));
        assertTrue(plain(player.nextComponentMessage()).contains("Platform: MockBukkit"));
        assertTrue(plain(player.nextComponentMessage()).contains("Adapter: modern"));
        assertTrue(plain(player.nextComponentMessage()).contains("Available: [ADVENTURE_TEXT"));
        assertEquals(1, player.getBossBars().size(), "modern servers show the boss bar");
    }

    @Test
    void selfTestPluginPassesOnMockBukkit() throws Exception {
        Plugin plugin = MockBukkit.loadJar(new File(System.getProperty("bloom.selftest.jar")));
        server.getPluginManager().enablePlugin(plugin);
        server.getScheduler().performTicks(60);
        server.getScheduler().waitAsyncTasksFinished();
        File results = new File(plugin.getDataFolder(), "results.json");
        for (int i = 0; i < 50 && !results.isFile(); i++) {
            Thread.sleep(100);
            server.getScheduler().performOneTick();
        }
        String json = new String(Files.readAllBytes(results.toPath()), "UTF-8");
        assertTrue(json.contains("\"result\": \"PASS\""), json);
    }
}
