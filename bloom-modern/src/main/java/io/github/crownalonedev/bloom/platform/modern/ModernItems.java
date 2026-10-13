package io.github.crownalonedev.bloom.platform.modern;

import io.github.crownalonedev.bloom.bukkit.internal.EnchantmentNames;
import io.github.crownalonedev.bloom.bukkit.platform.ItemPlatform;
import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** Item operations through the modern Paper API: Adventure names, PersistentDataContainer, byte serialization. */
final class ModernItems implements ItemPlatform {

    static final String SERIAL_PREFIX = "paper:";

    private final ModernText text;

    ModernItems(ModernText text) {
        this.text = text;
    }

    @Override
    public Optional<ItemStack> create(String materialName, short data, int amount) {
        if (data != 0) {
            return Optional.empty();
        }
        Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem() || material.isAir()) {
            return Optional.empty();
        }
        return Optional.of(new ItemStack(material, amount));
    }

    @Override
    public void displayName(ItemMeta meta, FormattedText name) {
        meta.displayName(text.itemComponent(name));
    }

    @Override
    public void lore(ItemMeta meta, List<FormattedText> lore) {
        List<Component> lines = new ArrayList<>(lore.size());
        for (FormattedText line : lore) {
            lines.add(text.itemComponent(line));
        }
        meta.lore(lines);
    }

    @Override
    public List<String> addItemFlags(ItemMeta meta, Collection<String> flagNames) {
        List<String> unknown = new ArrayList<>();
        for (String name : flagNames) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(name));
            } catch (IllegalArgumentException e) {
                unknown.add(name);
            }
        }
        return unknown;
    }

    @Override
    public void unbreakable(ItemMeta meta, boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
    }

    @Override
    public void customModelData(ItemMeta meta, int value) {
        meta.setCustomModelData(value);
    }

    @Override
    public void glow(ItemMeta meta) {
        meta.setEnchantmentGlintOverride(true);
    }

    @Override
    public Optional<Enchantment> enchantment(String name) {
        NamespacedKey key = NamespacedKey.minecraft(EnchantmentNames.toKey(name));
        return Optional.ofNullable(RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(key));
    }

    @Override
    public ItemStack writeData(ItemStack item, String namespace, String key, String value) {
        ItemStack copy = item.clone();
        ItemMeta meta = requireMeta(copy);
        meta.getPersistentDataContainer().set(key(namespace, key), PersistentDataType.STRING, value);
        copy.setItemMeta(meta);
        return copy;
    }

    @Override
    public Optional<String> readData(ItemStack item, String namespace, String key) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return Optional.ofNullable(container.get(key(namespace, key), PersistentDataType.STRING));
    }

    @Override
    public ItemStack removeData(ItemStack item, String namespace, String key) {
        ItemStack copy = item.clone();
        if (!copy.hasItemMeta()) {
            return copy;
        }
        ItemMeta meta = copy.getItemMeta();
        meta.getPersistentDataContainer().remove(key(namespace, key));
        copy.setItemMeta(meta);
        return copy;
    }

    @Override
    public String serialize(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            throw new IllegalArgumentException("Cannot serialize an empty item (air or amount 0).");
        }
        return SERIAL_PREFIX + Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    @Override
    public ItemStack deserialize(String data) {
        if (data == null || !data.startsWith(SERIAL_PREFIX)) {
            throw new IllegalArgumentException("Not a modern Bloom item string (expected prefix '" + SERIAL_PREFIX
                    + "'). Items serialized on legacy servers cannot be read on modern servers.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(data.substring(SERIAL_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed item data: invalid Base64.", e);
        }
        return ItemStack.deserializeBytes(bytes);
    }

    private static NamespacedKey key(String namespace, String key) {
        NamespacedKey namespaced = NamespacedKey.fromString(namespace + ":" + key);
        if (namespaced == null) {
            throw new IllegalArgumentException("Invalid item data key '" + namespace + ":" + key
                    + "'. Use lowercase letters, digits, '.', '_' and '-'.");
        }
        return namespaced;
    }

    private static ItemMeta requireMeta(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException("Item " + item.getType() + " cannot hold data (no item meta).");
        }
        return meta;
    }
}
