package io.github.crownalonedev.bloom.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.api.text.TextFormat;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.items.CustomItem;
import io.github.crownalonedev.bloom.items.ItemFlagKey;
import io.github.crownalonedev.bloom.items.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ItemsIT {

    private Bloom bloom;
    private Items items;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        bloom = Bloom.enable(MockBukkit.createMockPlugin("Items IT"));
        items = Items.of(bloom);
    }

    @AfterEach
    void tearDown() {
        bloom.shutdown();
        MockBukkit.unmock();
    }

    @Test
    void serviceIsSingletonPerBloom() {
        assertSame(items, Items.of(bloom));
        assertEquals("items_it", items.namespace());
    }

    @Test
    void resolvesModernAndLegacyMaterialNames() {
        assertEquals(Material.RED_WOOL, items.create("RED_WOOL", 1).get().getType());
        assertEquals(Material.RED_WOOL, items.create("WOOL:14", 1).get().getType());
        assertEquals(Material.CLOCK, items.create("WATCH", 1).get().getType());
        assertEquals(Material.PLAYER_HEAD, items.create("SKULL_ITEM:3", 2).get().getType());
        assertFalse(items.exists("NOT_A_MATERIAL"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> items.builder("NOPE"));
        assertTrue(e.getMessage().contains("Unknown material 'NOPE'"));
    }

    @Test
    void builderAppliesNameLoreEnchantsFlagsAndMeta() {
        ItemStack sword = items.builder("DIAMOND_SWORD")
                .name("&dBlossom {n}")
                .lore("&7Line one", "Plain line")
                .placeholders(Placeholder.of("n", "Blade"))
                .enchant("DAMAGE_ALL", 3)
                .enchant("knockback", 1)
                .flags(ItemFlagKey.HIDE_ENCHANTS, ItemFlagKey.HIDE_ADDITIONAL_TOOLTIP)
                .unbreakable(true)
                .customModelData(7)
                .build();
        ItemMeta meta = sword.getItemMeta();
        assertEquals("Blossom Blade", plain(meta.displayName()));
        assertEquals(NamedTextColor.LIGHT_PURPLE, firstColor(meta.displayName()));
        assertEquals(TextDecoration.State.FALSE, meta.displayName().decoration(TextDecoration.ITALIC));
        assertEquals("Plain line", plain(meta.lore().get(1)));
        assertEquals(NamedTextColor.WHITE, meta.lore().get(1).color(), "uncoloured lore defaults to white");
        assertEquals(3, sword.getEnchantmentLevel(Enchantment.SHARPNESS));
        assertEquals(1, sword.getEnchantmentLevel(Enchantment.KNOCKBACK));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP));
        assertTrue(meta.isUnbreakable());
        assertEquals(7, meta.getCustomModelData());
    }

    @Test
    void miniMessageFormatIsSupported() {
        ItemStack item = items.builder("PAPER")
                .format(TextFormat.MINIMESSAGE)
                .name("<gradient:#F2A7CC:#B9A0DD>Sakura")
                .build();
        assertEquals("Sakura", plain(item.getItemMeta().displayName()));
    }

    @Test
    void persistentDataAndCustomIdsSurviveCopies() {
        ItemStack plain = items.create("EMERALD", 1).get();
        ItemStack tagged = items.setData(plain, "tier", "3");
        assertNotSame(plain, tagged);
        assertFalse(items.getData(plain, "tier").isPresent(), "source item is not modified");
        assertEquals("3", items.getData(tagged.clone(), "tier").get());
        assertFalse(items.getData(items.removeData(tagged, "tier"), "tier").isPresent());

        CustomItem ruby =
                items.define("ruby", items.builder("RED_DYE").name("&cRuby").glow());
        ItemStack three = ruby.create(3);
        assertEquals(3, three.getAmount());
        assertTrue(ruby.matches(three));
        assertEquals(ruby, items.match(three).get());
        assertTrue(three.getItemMeta().getEnchantmentGlintOverride());
        assertFalse(ruby.matches(items.builder("RED_DYE").name("&cRuby").build()), "a look-alike is not a ruby");
        assertThrows(IllegalStateException.class, () -> items.define("ruby", items.builder("STONE")));
    }

    @Test
    void serializationRoundTrips() {
        ItemStack original = items.builder("GOLDEN_APPLE")
                .name("&6Golden")
                .amount(5)
                .customId("gapple")
                .build();
        String data = items.serialize(original);
        assertTrue(data.startsWith("paper:"));
        ItemStack restored = items.deserialize(data);
        assertTrue(restored.isSimilar(original));
        assertEquals(5, restored.getAmount());
        assertThrows(IllegalArgumentException.class, () -> items.deserialize("bukkit:abc"));
        assertThrows(IllegalArgumentException.class, () -> items.serialize(new ItemStack(Material.AIR)));
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static TextColor firstColor(Component component) {
        if (component.color() != null) {
            return component.color();
        }
        return component.children().isEmpty()
                ? null
                : component.children().get(0).color();
    }
}
