package io.github.crownalonedev.bloomexamples.docs;

import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.items.CustomItem;
import io.github.crownalonedev.bloom.items.ItemBuilder;
import io.github.crownalonedev.bloom.items.ItemFlagKey;
import io.github.crownalonedev.bloom.items.Items;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Item snippets used in the documentation. */
final class ItemSnippets {

    private ItemSnippets() {}

    static void builder(Bloom bloom, Player player) {
        // @snippet:items.builder
        Items items = Items.of(bloom);
        ItemStack sword = items.builder("DIAMOND_SWORD")
                .name("&dBlossom Blade")
                .lore("&7Owner: &f{owner}", "&7Forged under falling petals.")
                .placeholders(Placeholder.of("owner", player.getName()))
                .enchant("sharpness", 5) // modern key; "DAMAGE_ALL" works too
                .flags(ItemFlagKey.HIDE_ENCHANTS)
                .unbreakable(true)
                .glow()
                .build();
        player.getInventory().addItem(sword);
        // @end
    }

    static void materials(Bloom bloom) {
        // @snippet:items.materials
        Items items = Items.of(bloom);
        ItemStack wool = items.create("RED_WOOL", 16).orElseThrow(IllegalStateException::new); // WOOL:14 on 1.8
        ItemStack head = items.builder("PLAYER_HEAD").build(); // SKULL_ITEM:3 on 1.8
        boolean hasCopper = items.exists("COPPER_INGOT"); // false on 1.8, true on modern Paper

        // Your own keys, for items that differ between versions:
        items.materials().register("SHOP_ICON", "AMETHYST_SHARD", "DIAMOND", 0);
        ItemStack icon = items.builder("SHOP_ICON").name("&dShop").build();
        // @end
        wool.hashCode();
        head.hashCode();
        icon.hashCode();
        Boolean.valueOf(hasCopper).hashCode();
    }

    static void custom(Bloom bloom, Player player, ItemStack inHand) {
        // @snippet:items.custom
        Items items = Items.of(bloom);
        CustomItem ruby =
                items.define("ruby", items.builder("RED_DYE").name("&cRuby").glow());

        player.getInventory().addItem(ruby.create(3));

        if (ruby.matches(inHand)) { // checks persistent data, not the name: look-alikes with the same name don't match
            player.sendMessage("That's a real ruby.");
        }
        Optional<CustomItem> any = items.match(inHand); // which custom item is this, if any?
        // @end
        any.isPresent();
    }

    static void data(Bloom bloom, ItemStack stack) {
        // @snippet:items.data
        Items items = Items.of(bloom);
        ItemStack tagged = items.setData(stack, "upgrade-level", "3"); // returns a copy
        int level = Integer.parseInt(items.getData(tagged, "upgrade-level").orElse("0"));
        ItemStack plain = items.removeData(tagged, "upgrade-level");
        // @end
        plain.hashCode();
        Integer.valueOf(level).hashCode();
    }

    static void serialization(Bloom bloom, ItemStack stack) {
        // @snippet:items.serialize
        Items items = Items.of(bloom);
        String stored = items.serialize(stack); // "paper:..." on modern servers, "bukkit:..." on legacy servers
        ItemStack restored = items.deserialize(stored);
        // @end
        restored.hashCode();
    }

    static ItemStack capability(Bloom bloom) {
        // @snippet:items.capability
        ItemBuilder builder = Items.of(bloom).builder("STICK").name("&6Magic wand");
        if (bloom.has(Capability.CUSTOM_MODEL_DATA)) {
            builder.customModelData(4201); // resource-pack model on modern servers
        }
        return builder.build();
        // @end
    }
}
