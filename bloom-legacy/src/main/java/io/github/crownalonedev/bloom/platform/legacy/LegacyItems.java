package io.github.crownalonedev.bloom.platform.legacy;

import io.github.crownalonedev.bloom.bukkit.internal.EnchantmentNames;
import io.github.crownalonedev.bloom.bukkit.platform.ItemPlatform;
import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import io.github.crownalonedev.bloom.common.text.LegacyText;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

/** Item operations for 1.7.10 - 1.8.x. Persistent data is stored as NBT under {@code BloomData}. */
final class LegacyItems implements ItemPlatform {

    static final String SERIAL_PREFIX = "bukkit:";

    private final LegacyNms nms;
    private final boolean itemFlags;

    LegacyItems(LegacyNms nms, boolean itemFlags) {
        this.nms = nms;
        this.itemFlags = itemFlags;
    }

    @Override
    public Optional<ItemStack> create(String materialName, short data, int amount) {
        Material material = Material.matchMaterial(materialName);
        if (material == null || material == Material.AIR) {
            return Optional.empty();
        }
        return Optional.of(new ItemStack(material, amount, data));
    }

    @Override
    public void displayName(ItemMeta meta, FormattedText name) {
        meta.setDisplayName(itemText(name));
    }

    @Override
    public void lore(ItemMeta meta, List<FormattedText> lore) {
        List<String> lines = new ArrayList<String>(lore.size());
        for (FormattedText line : lore) {
            lines.add(itemText(line));
        }
        meta.setLore(lines);
    }

    /**
     * Item text starts with a reset when it has no leading formatting, so names render white and non-italic like on
     * modern servers (the 1.8 client italicises custom names and colours lore purple by default).
     */
    static String itemText(FormattedText text) {
        String rendered = LegacyTextPlatform.render(text);
        return rendered.isEmpty() || rendered.charAt(0) == LegacyText.SECTION ? rendered : "§r" + rendered;
    }

    @Override
    public List<String> addItemFlags(ItemMeta meta, Collection<String> flagNames) {
        return ItemFlagsV18.add(meta, flagNames);
    }

    @Override
    public void unbreakable(ItemMeta meta, boolean unbreakable) {
        UnbreakableV18.set(meta, unbreakable);
    }

    @Override
    public void customModelData(ItemMeta meta, int value) {
        throw new UnsupportedOperationException("Custom model data requires Minecraft 1.14+ (CUSTOM_MODEL_DATA).");
    }

    @Override
    public void glow(ItemMeta meta) {
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        if (itemFlags) {
            ItemFlagsV18.add(meta, java.util.Collections.singletonList("HIDE_ENCHANTS"));
        }
    }

    @Override
    public Optional<Enchantment> enchantment(String name) {
        return Optional.ofNullable(Enchantment.getByName(EnchantmentNames.toLegacy(name)));
    }

    @Override
    public ItemStack writeData(ItemStack item, String namespace, String key, String value) {
        return nms.writeString(item, namespace + ":" + key, value);
    }

    @Override
    public Optional<String> readData(ItemStack item, String namespace, String key) {
        if (item == null || item.getType() == Material.AIR) {
            return Optional.empty();
        }
        return nms.readString(item, namespace + ":" + key);
    }

    @Override
    public ItemStack removeData(ItemStack item, String namespace, String key) {
        return nms.removeString(item, namespace + ":" + key);
    }

    @Override
    public String serialize(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            throw new IllegalArgumentException("Cannot serialize an empty item (air or amount 0).");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes);
            out.writeObject(item);
            out.close();
            return SERIAL_PREFIX + Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialize item " + item.getType(), e);
        }
    }

    @Override
    public ItemStack deserialize(String data) {
        if (data == null || !data.startsWith(SERIAL_PREFIX)) {
            throw new IllegalArgumentException("Not a legacy Bloom item string (expected prefix '" + SERIAL_PREFIX
                    + "'). Items serialized on modern servers cannot be read on legacy servers.");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(data.substring(SERIAL_PREFIX.length()));
            BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes));
            try {
                return (ItemStack) in.readObject();
            } finally {
                in.close();
            }
        } catch (IOException | ClassNotFoundException | ClassCastException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed legacy item data.", e);
        }
    }
}
