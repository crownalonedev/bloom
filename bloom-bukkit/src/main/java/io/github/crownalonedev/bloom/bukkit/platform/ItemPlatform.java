package io.github.crownalonedev.bloom.bukkit.platform;

import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Adapter SPI for item operations that differ between versions.
 *
 * <p><strong>SPI:</strong> plugin code should use {@code bloom-items}. Methods guarded by a capability are only
 * called by Bloom when it is present.
 *
 * @since 0.1.0
 */
public interface ItemPlatform {

    /**
     * Creates an item from an exact material constant name of the running server.
     *
     * @param materialName exact {@code Material} constant name on this server, e.g. {@code "RED_WOOL"} (modern)
     *     or {@code "WOOL"} (legacy)
     * @param data legacy data/durability value; must be {@code 0} on platforms without
     *     {@code LEGACY_DATA_VALUES}
     * @param amount stack size
     * @return the item, or empty if the material does not exist on this server
     */
    Optional<ItemStack> create(String materialName, short data, int amount);

    /**
     * Sets the display name.
     *
     * @param meta item meta to modify
     * @param name prepared name
     */
    void displayName(ItemMeta meta, FormattedText name);

    /**
     * Replaces the lore.
     *
     * @param meta item meta to modify
     * @param lore prepared lore lines
     */
    void lore(ItemMeta meta, List<FormattedText> lore);

    /**
     * Adds item flags by Bukkit {@code ItemFlag} constant name. Requires {@code ITEM_FLAGS}.
     *
     * @param meta item meta to modify
     * @param flagNames constant names, e.g. {@code "HIDE_ENCHANTS"}
     * @return names that do not exist on this server and were skipped
     */
    List<String> addItemFlags(ItemMeta meta, Collection<String> flagNames);

    /**
     * Sets the unbreakable flag. Requires {@code UNBREAKABLE_ITEMS}.
     *
     * @param meta item meta to modify
     * @param unbreakable value
     */
    void unbreakable(ItemMeta meta, boolean unbreakable);

    /**
     * Sets custom model data. Requires {@code CUSTOM_MODEL_DATA}.
     *
     * @param meta item meta to modify
     * @param value model data value
     */
    void customModelData(ItemMeta meta, int value);

    /**
     * Makes the item glow like an enchanted item without showing an enchantment. Modern servers use the native
     * glint override; legacy servers add a hidden enchantment (visible on 1.7.10, which lacks item flags).
     *
     * @param meta item meta to modify
     */
    void glow(ItemMeta meta);

    /**
     * Resolves an enchantment by modern key ({@code "sharpness"}, {@code "minecraft:sharpness"}) or legacy Bukkit
     * name ({@code "DAMAGE_ALL"}).
     *
     * @param name enchantment key or name, case-insensitive
     * @return the enchantment, or empty if unknown on this server
     */
    Optional<Enchantment> enchantment(String name);

    /**
     * Writes a string value into the item's persistent data. Requires {@code PERSISTENT_DATA_CONTAINER} or
     * {@code ITEM_NBT_TAGS}.
     *
     * @param item source item (not modified)
     * @param namespace owning namespace, lowercase plugin name
     * @param key key within the namespace, {@code [a-z0-9._-]+}
     * @param value value to store
     * @return a copy of the item carrying the value
     */
    ItemStack writeData(ItemStack item, String namespace, String key, String value);

    /**
     * Reads a string value from the item's persistent data.
     *
     * @param item item to read
     * @param namespace owning namespace
     * @param key key within the namespace
     * @return the value, or empty if absent
     */
    Optional<String> readData(ItemStack item, String namespace, String key);

    /**
     * Removes a value from the item's persistent data.
     *
     * @param item source item (not modified)
     * @param namespace owning namespace
     * @param key key within the namespace
     * @return a copy of the item without the value
     */
    ItemStack removeData(ItemStack item, String namespace, String key);

    /**
     * Serialises an item to a portable string for storage on the same platform family.
     *
     * @param item item to serialise
     * @return serialised form with a format prefix
     */
    String serialize(ItemStack item);

    /**
     * Deserialises an item produced by {@link #serialize(ItemStack)} on the same platform family.
     *
     * @param data serialised form
     * @return the item
     * @throws IllegalArgumentException if the data is malformed or from another family
     */
    ItemStack deserialize(String data);
}
