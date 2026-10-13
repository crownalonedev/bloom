package io.github.crownalonedev.bloomexamples.docs;

import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.gui.Button;
import io.github.crownalonedev.bloom.gui.ConfirmMenu;
import io.github.crownalonedev.bloom.gui.Menu;
import io.github.crownalonedev.bloom.gui.MenuTemplate;
import io.github.crownalonedev.bloom.gui.MenuTemplates;
import io.github.crownalonedev.bloom.gui.Menus;
import io.github.crownalonedev.bloom.gui.StateKey;
import io.github.crownalonedev.bloom.items.Items;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** GUI snippets used in the documentation. */
final class GuiSnippets {

    private GuiSnippets() {}

    static void basic(Bloom bloom, Player player) {
        // @snippet:gui.basic
        Items items = Items.of(bloom);
        Menu menu = Menu.builder()
                .title("&dWarps")
                .rows(3)
                .button(
                        11,
                        Button.of(items.builder("GRASS_BLOCK").name("&aSpawn").build(), click -> {
                            click.player().performCommand("spawn");
                            click.close();
                        }))
                .button(
                        15,
                        Button.of(
                                items.builder("DIAMOND_SWORD").name("&cArena").build(),
                                click -> click.player().performCommand("warp arena")))
                .fill(Button.of(
                        items.builder("GRAY_STAINED_GLASS_PANE").name(" ").build()))
                .build();

        Menus.of(bloom).open(player, menu);
        // @end
    }

    static Menu layout(Bloom bloom, Button heal, Button feed, Button exit) {
        // @snippet:gui.layout
        Items items = Items.of(bloom);
        return Menu.builder()
                .title("&d&lBlossom Shop")
                .layout("#########", "#  h f  #", "####x####")
                .bind(
                        '#',
                        Button.of(items.builder("PINK_STAINED_GLASS_PANE")
                                .name(" ")
                                .build()))
                .bind('h', heal)
                .bind('f', feed)
                .bind('x', exit)
                .build();
        // @end
    }

    static Menu buttons(Bloom bloom) {
        Items items = Items.of(bloom);
        ItemStack lockedIcon = items.builder("BARRIER").name("&cVIP only").build();
        ItemStack vipIcon = items.builder("DIAMOND").name("&bVIP").build();
        // @snippet:gui.buttons
        Button vip = Button.of(vipIcon, click -> click.player().sendMessage("Welcome, VIP"))
                .permission("shop.vip", lockedIcon); // players without the permission see lockedIcon

        Button nightOnly = Button.of(
                        items.builder("CLOCK").name("&9Night market").build())
                .visibleWhen(view -> view.player().getWorld().getTime() > 13000);

        Button health = Button.dynamic(view -> items.builder("GOLDEN_APPLE")
                .name("&aHealth: " + (int) view.player().getHealth())
                .build());
        // @end
        return Menu.builder()
                .rows(1)
                .button(0, vip)
                .button(1, nightOnly)
                .button(2, health)
                .build();
    }

    static final StateKey<Integer> QUANTITY = StateKey.of("quantity", Integer.class);

    static Menu state(Bloom bloom) {
        Items items = Items.of(bloom);
        // @snippet:gui.state
        Button quantity = Button.dynamic(view -> items.builder("GOLD_NUGGET")
                        .name("&eQuantity: " + view.state().getOrDefault(QUANTITY, 1))
                        .lore("&7Left click +1, right click -1")
                        .build())
                .onClick(click -> {
                    int current = click.view().state().getOrDefault(QUANTITY, 1);
                    int next = click.kind().isRight() ? Math.max(1, current - 1) : current + 1;
                    click.view().state().set(QUANTITY, next);
                    click.refresh(); // re-renders this player's view only
                });
        // @end
        return Menu.builder().rows(1).button(4, quantity).refreshEvery(20).build();
    }

    static Menu paginated(Bloom bloom, List<String> warps) {
        Items items = Items.of(bloom);
        // @snippet:gui.paginated
        Menu warpList = Menu.builder()
                .title("&dWarps &8(page {page} of {pages})")
                .apply(MenuTemplates.paged(
                        6,
                        Button.of(items.builder("BLACK_STAINED_GLASS_PANE")
                                .name(" ")
                                .build()),
                        items.builder("ARROW").name("&7Previous").build(),
                        items.builder("ARROW").name("&7Next").build(),
                        Button.of(items.builder("BARRIER").name("&cBack").build(), click -> click.back())))
                .paginate(view -> {
                    List<Button> buttons = new ArrayList<Button>();
                    for (final String warp : warps) {
                        buttons.add(Button.of(
                                items.builder("ENDER_PEARL").name("&f" + warp).build(),
                                click -> click.player().performCommand("warp " + warp)));
                    }
                    return buttons;
                })
                .build();
        // @end
        return warpList;
    }

    static void confirm(Bloom bloom, Player player, final String home) {
        Items items = Items.of(bloom);
        // @snippet:gui.confirm
        Menu confirm = ConfirmMenu.builder()
                .title("&cDelete home " + home + "?")
                .confirm(items.builder("LIME_WOOL").name("&aDelete").build(), click -> {
                    click.player().performCommand("delhome " + home);
                    click.close();
                })
                .cancel(items.builder("RED_WOOL").name("&cKeep it").build()) // default: back, or close
                .build();
        Menus.of(bloom).open(player, confirm);
        // @end
    }

    static Menu navigation(Bloom bloom, final Menu settings) {
        Items items = Items.of(bloom);
        // @snippet:gui.navigation
        Menu main = Menu.builder()
                .title("&dMenu")
                .rows(1)
                .button(
                        4,
                        Button.of(
                                items.builder("COMPARATOR").name("&7Settings").build(),
                                click -> click.open(settings))) // opens next tick; this menu goes into the history
                .build();
        // In the settings menu: Button.of(backIcon, click -> click.back())
        // @end
        return main;
    }

    static void templates(Bloom bloom) {
        Items items = Items.of(bloom);
        final ItemStack logo =
                items.builder("PINK_TULIP").name("&dBloom Network").build();
        // @snippet:gui.templates
        MenuTemplate branded = builder -> builder.border(Button.of(
                        items.builder("PINK_STAINED_GLASS_PANE").name(" ").build()))
                .item(4, logo);

        Menu profile = Menu.builder().rows(5).apply(branded).title("&dProfile").build();
        // @end
        profile.size();
    }

    static void service(Bloom bloom) {
        // @snippet:gui.service
        Menus menus = Menus.of(bloom).clickDebounce(Duration.ofMillis(150)); // default 100 ms
        // @end
        menus.openCount();
    }
}
