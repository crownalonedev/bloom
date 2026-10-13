package io.github.crownalonedev.bloomexamples.items;

import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.items.CustomItem;
import io.github.crownalonedev.bloom.items.ItemBuilder;
import io.github.crownalonedev.bloom.items.ItemFlagKey;
import io.github.crownalonedev.bloom.items.Items;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bloom Items example: custom items identified by persistent data (not by name), cross-version material names and
 * capability-aware features.
 */
public final class ItemsExamplePlugin extends JavaPlugin {

    private Bloom bloom;
    private CustomItem ruby;
    private CustomItem blossomBlade;

    @Override
    public void onEnable() {
        bloom = Bloom.enable(this);
        Items items = Items.of(bloom);

        // RED_DYE is a modern name; Bloom maps it to INK_SACK:1 on 1.7/1.8 servers.
        ruby = items.define(
                "ruby",
                items.builder("RED_DYE")
                        .name("&cRuby")
                        .lore("&7A rare gem.", "&8Right-click to admire it.")
                        .glow());

        ItemBuilder blade = items.builder("DIAMOND_SWORD")
                .name("&dBlossom Blade")
                .lore("&7Forged under falling petals.")
                .enchant("sharpness", 5)
                .flags(ItemFlagKey.HIDE_ENCHANTS)
                .unbreakable(true);
        if (bloom.has(Capability.CUSTOM_MODEL_DATA)) {
            blade.customModelData(1001); // resource-pack model on modern servers only
        }
        blossomBlade = items.define("blossom_blade", blade);

        bloom.events()
                .listen(PlayerInteractEvent.class)
                .filter(event ->
                        event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                .filter(event -> ruby.matches(event.getItem()))
                .handler(event -> bloom.messages().send(event.getPlayer(), "&cThe ruby sparkles."));

        getCommand("bloomitems").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            bloom.messages().send(sender, "&cOnly players can receive items.");
            return true;
        }
        Player player = (Player) sender;
        player.getInventory().addItem(ruby.create(3), blossomBlade.create());
        bloom.messages().send(player, "&aYou received 3 rubies and the Blossom Blade.");
        return true;
    }

    @Override
    public void onDisable() {
        if (bloom != null) {
            bloom.shutdown();
        }
    }
}
