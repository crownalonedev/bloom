package io.github.crownalonedev.bloom.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.gui.Button;
import io.github.crownalonedev.bloom.gui.ClickKind;
import io.github.crownalonedev.bloom.gui.ConfirmMenu;
import io.github.crownalonedev.bloom.gui.Menu;
import io.github.crownalonedev.bloom.gui.MenuTemplates;
import io.github.crownalonedev.bloom.gui.MenuView;
import io.github.crownalonedev.bloom.gui.Menus;
import io.github.crownalonedev.bloom.gui.StateKey;
import io.github.crownalonedev.bloom.items.Items;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class MenusIT {

    private static final StateKey<Integer> CLICKS = StateKey.of("clicks", Integer.class);

    private ServerMock server;
    private Plugin plugin;
    private Bloom bloom;
    private Menus menus;
    private Items items;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("MenusIT");
        bloom = Bloom.enable(plugin);
        menus = Menus.of(bloom).clickDebounce(Duration.ZERO);
        items = Items.of(bloom);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        if (bloom != null) {
            bloom.shutdown();
        }
        MockBukkit.unmock();
    }

    private ItemStack item(String material, String name) {
        return items.builder(material).name(name).build();
    }

    private InventoryClickEvent click(int rawSlot, ClickType type) {
        return player.simulateInventoryClick(player.getOpenInventory(), type, rawSlot);
    }

    private void tick() {
        server.getScheduler().performOneTick();
    }

    @Test
    void opensMenuAndRendersButtonsAndFiller() {
        Menu menu = Menu.builder()
                .title("&dShop")
                .rows(3)
                .button(13, Button.of(item("DIAMOND", "&bGem")))
                .fill(Button.of(item("GRAY_STAINED_GLASS_PANE", " ")))
                .build();
        MenuView view = menus.open(player, menu);
        assertTrue(view.isOpen());
        InventoryView open = player.getOpenInventory();
        assertEquals(27, open.getTopInventory().getSize());
        assertEquals(Material.DIAMOND, open.getTopInventory().getItem(13).getType());
        assertEquals(
                Material.GRAY_STAINED_GLASS_PANE,
                open.getTopInventory().getItem(0).getType());
        assertEquals(1, menus.openCount());
        assertSame(view, menus.view(player).get());
    }

    @Test
    void clicksRunHandlersAndAreAlwaysCancelled() {
        AtomicReference<ClickKind> seen = new AtomicReference<>();
        Menu menu = Menu.builder()
                .rows(1)
                .button(4, Button.of(item("EMERALD", "&aBuy"), click -> seen.set(click.kind())))
                .build();
        menus.open(player, menu);

        InventoryClickEvent event = click(4, ClickType.RIGHT);
        assertTrue(event.isCancelled());
        assertEquals(ClickKind.RIGHT, seen.get());

        seen.set(null);
        InventoryClickEvent empty = click(0, ClickType.LEFT);
        assertTrue(empty.isCancelled(), "clicks on empty slots are cancelled");
        assertNull(seen.get());
    }

    @Test
    void duplicationVectorsAreBlocked() {
        AtomicInteger handled = new AtomicInteger();
        Menu menu = Menu.builder()
                .rows(1)
                .button(0, Button.of(item("DIAMOND", "&bGem"), click -> handled.incrementAndGet()))
                .build();
        player.getInventory().setItem(0, new ItemStack(Material.STONE, 64));
        menus.open(player, menu);

        // Shift-click from the player's own inventory (raw slot >= menu size) must not move items into the menu.
        InventoryClickEvent shiftFromBottom = click(9 + 27, ClickType.SHIFT_LEFT);
        assertTrue(shiftFromBottom.isCancelled());
        // Number-key swap onto a menu slot.
        assertTrue(click(0, ClickType.NUMBER_KEY).isCancelled());
        // Double click (collect to cursor) is cancelled and not dispatched a second time.
        assertTrue(click(0, ClickType.DOUBLE_CLICK).isCancelled());
        assertEquals(1, handled.get(), "NUMBER_KEY dispatched once, DOUBLE_CLICK never");

        // Drag across menu slots.
        InventoryView view = player.getOpenInventory();
        InventoryDragEvent drag = new InventoryDragEvent(
                view,
                null,
                new ItemStack(Material.STONE, 2),
                false,
                Collections.singletonMap(0, new ItemStack(Material.STONE, 1)));
        server.getPluginManager().callEvent(drag);
        assertTrue(drag.isCancelled());

        assertEquals(Material.DIAMOND, view.getTopInventory().getItem(0).getType());
        assertEquals(64, player.getInventory().getItem(0).getAmount());
    }

    @Test
    void otherPluginsCannotUncancelMenuClicks() {
        Menu menu = Menu.builder().rows(1).item(0, item("DIAMOND", "x")).build();
        menus.open(player, menu);
        bloom.events()
                .listen(InventoryClickEvent.class)
                .priority(org.bukkit.event.EventPriority.HIGH)
                .handler(event -> event.setCancelled(false));
        assertTrue(click(0, ClickType.LEFT).isCancelled());
    }

    @Test
    void permissionButtonsHideOrShowDeniedItem() {
        Menu menu = Menu.builder()
                .rows(1)
                .button(0, Button.of(item("DIAMOND", "vip")).permission("test.vip"))
                .button(
                        1,
                        Button.of(item("DIAMOND", "vip2"), c -> {}).permission("test.vip", item("BARRIER", "&cLocked")))
                .build();
        menus.open(player, menu);
        assertNull(player.getOpenInventory().getTopInventory().getItem(0));
        assertEquals(
                Material.BARRIER,
                player.getOpenInventory().getTopInventory().getItem(1).getType());

        PermissionAttachment attachment = player.addAttachment(plugin, "test.vip", true);
        menus.refresh(player);
        assertEquals(
                Material.DIAMOND,
                player.getOpenInventory().getTopInventory().getItem(0).getType());
        assertEquals(
                Material.DIAMOND,
                player.getOpenInventory().getTopInventory().getItem(1).getType());
        player.removeAttachment(attachment);
    }

    @Test
    void paginationSlicesContentAndUpdatesTitle() {
        List<Button> content = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            content.add(Button.of(new ItemStack(Material.PAPER, i + 1)));
        }
        Menu menu = Menu.builder()
                .title("Page {page}/{pages}")
                .apply(MenuTemplates.paged(
                        3,
                        Button.of(item("BLACK_STAINED_GLASS_PANE", " ")),
                        item("ARROW", "Prev"),
                        item("ARROW", "Next"),
                        null))
                .paginate(view -> content)
                .build();
        MenuView view = menus.open(player, menu);
        // 3 rows with a border leave 7 content slots: 20 items -> 3 pages.
        assertEquals(3, view.pageCount());
        assertEquals(1, player.getOpenInventory().getTopInventory().getItem(10).getAmount());
        assertEquals(
                Material.BLACK_STAINED_GLASS_PANE,
                player.getOpenInventory().getTopInventory().getItem(18).getType(),
                "no previous button on the first page");
        assertEquals(
                Material.ARROW,
                player.getOpenInventory().getTopInventory().getItem(26).getType());

        click(26, ClickType.LEFT); // next page -> title changes -> reopened next tick
        tick();
        assertEquals(1, view.page());
        assertTrue(view.isOpen());
        assertEquals(8, player.getOpenInventory().getTopInventory().getItem(10).getAmount());
        assertEquals(
                Material.ARROW,
                player.getOpenInventory().getTopInventory().getItem(18).getType());
        assertEquals("Page 2/3", plainTitle());

        view.page(99);
        tick();
        assertEquals(2, view.page(), "pages are clamped");
        assertEquals(
                Material.BLACK_STAINED_GLASS_PANE,
                player.getOpenInventory().getTopInventory().getItem(26).getType());
    }

    @Test
    void navigationKeepsHistoryAndBackReturns() {
        AtomicInteger closedA = new AtomicInteger();
        Menu b = Menu.builder()
                .title("B")
                .rows(1)
                .button(0, Button.of(item("ARROW", "Back"), click -> click.back()))
                .build();
        Menu a = Menu.builder()
                .title("A")
                .rows(1)
                .button(0, Button.of(item("COMPASS", "Go"), click -> click.open(b)))
                .onClose(view -> closedA.incrementAndGet())
                .build();
        menus.open(player, a);
        click(0, ClickType.LEFT);
        tick();
        assertEquals("B", plainTitle());
        assertEquals(1, closedA.get(), "A's onClose runs when navigating away");

        click(0, ClickType.LEFT);
        tick();
        assertEquals("A", plainTitle());
        assertTrue(menus.view(player).get().isOpen());
        assertFalse(menus.back(player), "history is empty after going back");
    }

    @Test
    void closingClearsSessionAndRunsOnClose() {
        AtomicInteger closes = new AtomicInteger();
        Menu menu =
                Menu.builder().rows(1).onClose(view -> closes.incrementAndGet()).build();
        menus.open(player, menu);
        player.closeInventory();
        assertEquals(1, closes.get());
        assertEquals(0, menus.openCount());
        assertFalse(menus.view(player).isPresent());
    }

    @Test
    void stateSurvivesRefreshes() {
        Menu menu = Menu.builder()
                .rows(1)
                .button(
                        0,
                        Button.dynamic(view -> new ItemStack(
                                        Material.PAPER, view.state().getOrDefault(CLICKS, 0) + 1))
                                .onClick(click -> {
                                    click.view()
                                            .state()
                                            .set(CLICKS, click.view().state().getOrDefault(CLICKS, 0) + 1);
                                    click.refresh();
                                }))
                .build();
        menus.open(player, menu);
        click(0, ClickType.LEFT);
        click(0, ClickType.LEFT);
        assertEquals(3, player.getOpenInventory().getTopInventory().getItem(0).getAmount());
    }

    @Test
    void debounceSuppressesRapidClicks() {
        menus.clickDebounce(Duration.ofSeconds(10));
        AtomicInteger purchases = new AtomicInteger();
        Menu menu = Menu.builder()
                .rows(1)
                .button(0, Button.of(item("GOLD_INGOT", "Buy"), click -> purchases.incrementAndGet()))
                .build();
        menus.open(player, menu);
        click(0, ClickType.LEFT);
        click(0, ClickType.LEFT);
        click(0, ClickType.LEFT);
        assertEquals(1, purchases.get());
    }

    @Test
    void confirmMenuRunsActions() {
        AtomicInteger confirmed = new AtomicInteger();
        Menu confirm = ConfirmMenu.builder()
                .title("Sure?")
                .confirm(item("LIME_WOOL", "Yes"), click -> {
                    confirmed.incrementAndGet();
                    click.close();
                })
                .cancel(item("RED_WOOL", "No"))
                .build();
        menus.open(player, confirm);
        click(11, ClickType.LEFT);
        tick();
        assertEquals(1, confirmed.get());
        assertEquals(0, menus.openCount());

        menus.open(player, confirm);
        click(15, ClickType.LEFT); // default cancel: no history -> close
        tick();
        assertEquals(0, menus.openCount());
    }

    @Test
    void refreshEveryUpdatesWhileOpen() {
        AtomicInteger renders = new AtomicInteger();
        Menu menu = Menu.builder()
                .rows(1)
                .refreshEvery(5)
                .button(
                        0,
                        Button.dynamic(view -> new ItemStack(Material.CLOCK, Math.min(64, renders.incrementAndGet()))))
                .build();
        menus.open(player, menu);
        server.getScheduler().performTicks(10);
        assertEquals(3, renders.get());
        player.closeInventory();
        server.getScheduler().performTicks(20);
        assertEquals(3, renders.get(), "refresh stops when closed");
    }

    @Test
    void quitAndShutdownCleanUp() {
        AtomicInteger closes = new AtomicInteger();
        Menu menu =
                Menu.builder().rows(1).onClose(view -> closes.incrementAndGet()).build();
        menus.open(player, menu);
        server.getPluginManager()
                .callEvent(new PlayerQuitEvent(player, Component.empty(), PlayerQuitEvent.QuitReason.DISCONNECTED));
        assertEquals(1, closes.get());
        assertEquals(0, menus.openCount());

        PlayerMock other = server.addPlayer();
        menus.open(other, menu);
        bloom.shutdown();
        bloom = null;
        assertEquals(2, closes.get());
        org.bukkit.inventory.Inventory top = other.getOpenInventory().getTopInventory();
        assertTrue(
                top == null || top.getType() == org.bukkit.event.inventory.InventoryType.CRAFTING,
                "open menus are closed on shutdown");
    }

    @Test
    void builderValidation() {
        assertThrows(IllegalArgumentException.class, () -> Menu.builder().rows(7));
        assertThrows(IllegalArgumentException.class, () -> Menu.builder().layout("short"));
        assertThrows(
                IllegalStateException.class,
                () -> Menu.builder().layout("#########").build());
        assertThrows(
                IllegalArgumentException.class,
                () -> Menu.builder()
                        .rows(1)
                        .button(9, Button.of(new ItemStack(Material.STONE)))
                        .build());
        Menu layout = Menu.builder()
                .layout("#   x   #")
                .bind('#', Button.of(new ItemStack(Material.STONE)))
                .bind('x', Button.of(new ItemStack(Material.DIAMOND)))
                .build();
        assertEquals(1, layout.rows());
        menus.open(player, layout);
        assertEquals(
                Material.DIAMOND,
                player.getOpenInventory().getTopInventory().getItem(4).getType());
    }

    @Test
    void openRequiresMainThread() throws Exception {
        Menu menu = Menu.builder().rows(1).build();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                menus.open(player, menu);
            } catch (Throwable t) {
                error.set(t);
            }
        });
        thread.start();
        thread.join();
        assertTrue(error.get() instanceof IllegalStateException);
    }

    private String plainTitle() {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(player.getOpenInventory().title());
    }
}
