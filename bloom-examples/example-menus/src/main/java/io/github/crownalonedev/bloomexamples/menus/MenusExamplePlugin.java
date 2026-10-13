package io.github.crownalonedev.bloomexamples.menus;

import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.gui.Button;
import io.github.crownalonedev.bloom.gui.ConfirmMenu;
import io.github.crownalonedev.bloom.gui.Menu;
import io.github.crownalonedev.bloom.gui.MenuTemplates;
import io.github.crownalonedev.bloom.gui.Menus;
import io.github.crownalonedev.bloom.items.Items;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Bloom GUI example: a layout-based shop, a paginated player list and a confirmation dialog. */
public final class MenusExamplePlugin extends JavaPlugin {

    private Bloom bloom;
    private Menus menus;
    private Items items;
    private Menu shop;

    @Override
    public void onEnable() {
        bloom = Bloom.enable(this);
        menus = Menus.of(bloom);
        items = Items.of(bloom);
        shop = buildShop();
        getCommand("shop").setExecutor(this);
        getCommand("players").setExecutor(this);
    }

    private Menu buildShop() {
        Button pane =
                Button.of(items.builder("PINK_STAINED_GLASS_PANE").name(" ").build());
        Button heal = Button.of(items.builder("GOLDEN_APPLE")
                        .name("&aHeal")
                        .lore("&7Restores your health.")
                        .build())
                .onClick(click -> {
                    click.player().setHealth(click.player().getMaxHealth());
                    bloom.messages().send(click.player(), "&aYou feel refreshed.");
                    click.close();
                });
        Button feed = Button.of(items.builder("COOKED_BEEF")
                        .name("&6Feed")
                        .lore("&7Fills your hunger bar.")
                        .build())
                .onClick(click -> {
                    click.player().setFoodLevel(20);
                    click.refresh();
                });
        Button vip = Button.of(
                        items.builder("DIAMOND").name("&bVIP Lounge").glow().build(),
                        click -> bloom.messages().send(click.player(), "&bWelcome to the lounge!"))
                .permission(
                        "bloomexample.vip",
                        items.builder("BARRIER").name("&cVIP only").build());
        Button players = Button.of(
                items.builder("PLAYER_HEAD").name("&dOnline players").build(), click -> click.open(playerList()));

        return Menu.builder()
                .title("&d&lBlossom Shop")
                .layout("#########", "# h f v #", "####p####")
                .bind('#', pane)
                .bind('h', heal)
                .bind('f', feed)
                .bind('v', vip)
                .bind('p', players)
                .build();
    }

    private Menu playerList() {
        ItemStack previous = items.builder("ARROW").name("&7Previous page").build();
        ItemStack next = items.builder("ARROW").name("&7Next page").build();
        Button close = Button.of(items.builder("BARRIER").name("&cBack").build(), click -> click.back());
        Button border =
                Button.of(items.builder("GRAY_STAINED_GLASS_PANE").name(" ").build());
        return Menu.builder()
                .title("&dPlayers &8({page}/{pages})")
                .apply(MenuTemplates.paged(6, border, previous, next, close))
                .paginate(view -> {
                    List<Button> heads = new ArrayList<Button>();
                    for (final Player target : bloom.adapter().players().onlinePlayers()) {
                        heads.add(Button.of(items.builder("PLAYER_HEAD")
                                        .name("&f{name}")
                                        .placeholders(Placeholder.of("name", target.getName()))
                                        .lore("&7Click to teleport")
                                        .build())
                                .onClick(click -> click.open(confirmTeleport(target))));
                    }
                    return heads;
                })
                .build();
    }

    private Menu confirmTeleport(final Player target) {
        return ConfirmMenu.builder()
                .title("&8Teleport to " + target.getName() + "?")
                .confirm(items.builder("LIME_WOOL").name("&aTeleport").build(), click -> {
                    if (target.isOnline()) {
                        click.player().teleport(target);
                    }
                    click.close();
                })
                .cancel(items.builder("RED_WOOL").name("&cCancel").build())
                .fill(Button.of(
                        items.builder("BLACK_STAINED_GLASS_PANE").name(" ").build()))
                .build();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            bloom.messages().send(sender, "&cOnly players can open menus.");
            return true;
        }
        Player player = (Player) sender;
        menus.open(player, command.getName().equalsIgnoreCase("shop") ? shop : playerList());
        return true;
    }

    @Override
    public void onDisable() {
        if (bloom != null) {
            bloom.shutdown(); // closes open menus
        }
    }
}
