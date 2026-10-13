package io.github.crownalonedev.bloom.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crownalonedev.bloom.api.lifecycle.LifecycleState;
import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.api.platform.PlatformFamily;
import io.github.crownalonedev.bloom.api.platform.ServerSoftware;
import io.github.crownalonedev.bloom.api.scheduler.ScheduledTask;
import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.api.text.TextFormat;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.bukkit.BloomOptions;
import io.github.crownalonedev.bloom.bukkit.event.Subscription;
import io.github.crownalonedev.bloom.bukkit.text.BloomBossBar;
import io.github.crownalonedev.bloom.bukkit.text.BossBarColor;
import io.github.crownalonedev.bloom.bukkit.text.BossBarStyle;
import io.github.crownalonedev.bloom.bukkit.text.Messenger;
import io.github.crownalonedev.bloom.bukkit.text.Title;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class BloomCoreIT {

    private ServerMock server;
    private Plugin plugin;
    private Bloom bloom;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("BloomIT");
        bloom = Bloom.enable(plugin);
    }

    @AfterEach
    void tearDown() {
        if (bloom != null) {
            bloom.shutdown();
        }
        MockBukkit.unmock();
    }

    @Test
    void detectsMockBukkitAndSelectsModernAdapter() {
        assertEquals(LifecycleState.RUNNING, bloom.state());
        assertEquals(ServerSoftware.MOCKBUKKIT, bloom.platform().software());
        assertEquals(PlatformFamily.MODERN, bloom.adapter().family());
        assertTrue(bloom.has(Capability.MINIMESSAGE));
        assertTrue(bloom.has(Capability.PERSISTENT_DATA_CONTAINER));
        assertFalse(bloom.has(Capability.LEGACY_DATA_VALUES));
        assertSame(bloom, Bloom.get());
    }

    @Test
    void enablingTwiceFailsAndShutdownAllowsReEnable() {
        assertThrows(IllegalStateException.class, () -> Bloom.enable(plugin));
        bloom.shutdown();
        assertEquals(LifecycleState.STOPPED, bloom.state());
        assertThrows(IllegalStateException.class, Bloom::get);
        assertThrows(IllegalStateException.class, () -> bloom.scheduler());
        bloom = Bloom.enable(plugin);
        assertEquals(LifecycleState.RUNNING, bloom.state());
    }

    @Test
    void schedulerRunsTasksOnTicksAndCancelsOnShutdown() {
        AtomicInteger runs = new AtomicInteger();
        bloom.scheduler().syncLater(runs::incrementAndGet, 5);
        ScheduledTask repeating = bloom.scheduler().syncRepeating(task -> runs.incrementAndGet(), 0, 10);
        server.getScheduler().performTicks(5);
        assertEquals(2, runs.get(), "one-shot at tick 5 plus repeating at tick 0");
        bloom.shutdown();
        assertTrue(repeating.isCancelled());
        server.getScheduler().performTicks(40);
        assertEquals(2, runs.get());
    }

    @Test
    void supplySyncCompletesOnMainThread() throws Exception {
        CompletableFuture<String> future = bloom.scheduler().supplySync(() -> "done");
        server.getScheduler().performTicks(1);
        assertEquals("done", future.get());
    }

    @Test
    void messengerRendersLegacyAndMiniMessageWithSafePlaceholders() {
        PlayerMock player = server.addPlayer();
        bloom.messages().send(player, "&dHi {name}", Placeholder.of("name", "&cBob"));
        Component legacy = player.nextComponentMessage();
        assertEquals("Hi &cBob", plain(legacy));

        Messenger mini = bloom.messages().withFormat(TextFormat.MINIMESSAGE).withPrefix("<gray>[IT] ");
        mini.send(player, "<light_purple>Hello {name}", Placeholder.of("name", "<red>Eve"));
        Component component = player.nextComponentMessage();
        assertEquals("[IT] Hello <red>Eve", plain(component));
    }

    @Test
    void actionBarTitleAndBossBarUseAdventure() {
        PlayerMock player = server.addPlayer();
        assertTrue(bloom.messages().actionBar(player, "&aSaved"));
        Component bar = player.nextActionBar();
        assertNotNull(bar);
        assertEquals("Saved", plain(bar));
        assertEquals(
                NamedTextColor.GREEN,
                bar.children().isEmpty() ? bar.color() : bar.children().get(0).color());

        assertTrue(bloom.messages().title(player, Title.of("&dWelcome", "&7{who}"), Placeholder.of("who", "friend")));

        BloomBossBar bossBar = bloom.messages().bossBar("&dEvent", 0.5f, BossBarColor.PINK, BossBarStyle.SOLID);
        assertTrue(bossBar.isFunctional());
        bossBar.show(player);
        assertEquals(1, player.getBossBars().size());
        bloom.shutdown();
        assertTrue(bossBar.isClosed());
        assertEquals(0, player.getBossBars().size(), "boss bars are hidden on shutdown");
    }

    @Test
    void eventSubscriptionsFilterLimitAndUnregisterOnShutdown() {
        AtomicInteger joins = new AtomicInteger();
        Subscription limited = bloom.events()
                .listen(PlayerJoinEvent.class)
                .filter(event -> event.getPlayer().getName().startsWith("Player"))
                .limit(2)
                .handler(event -> joins.incrementAndGet());
        AtomicInteger all = new AtomicInteger();
        Subscription unlimited = bloom.events().subscribe(PlayerJoinEvent.class, event -> all.incrementAndGet());

        server.addPlayer();
        server.addPlayer();
        server.addPlayer();
        assertEquals(2, joins.get());
        assertFalse(limited.isActive());
        assertEquals(3, all.get());

        bloom.shutdown();
        assertFalse(unlimited.isActive());
        server.addPlayer();
        assertEquals(3, all.get());
    }

    @Test
    void optionsAreApplied() {
        bloom.shutdown();
        bloom = Bloom.enable(
                plugin,
                BloomOptions.builder()
                        .textFormat(TextFormat.MINIMESSAGE)
                        .quietStartup(true)
                        .build());
        assertEquals(TextFormat.MINIMESSAGE, bloom.messages().format());
    }

    @Test
    void servicesAreClosedOnShutdown() {
        AtomicInteger closed = new AtomicInteger();
        bloom.services().register(AutoCloseable.class, closed::incrementAndGet);
        bloom.shutdown();
        assertEquals(1, closed.get());
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
