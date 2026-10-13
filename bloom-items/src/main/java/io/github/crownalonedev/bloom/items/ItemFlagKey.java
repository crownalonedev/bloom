package io.github.crownalonedev.bloom.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Cross-version item flags. Each key maps to the Bukkit {@code ItemFlag} constant(s) that implement it on different
 * versions; flags that do not exist on the running server are skipped.
 *
 * <p>Requires {@code Capability.ITEM_FLAGS} (1.8+).
 *
 * @since 0.1.0
 */
public enum ItemFlagKey {
    /** Hides enchantments. */
    HIDE_ENCHANTS("HIDE_ENCHANTS"),
    /** Hides attribute modifiers such as attack damage. */
    HIDE_ATTRIBUTES("HIDE_ATTRIBUTES"),
    /** Hides the "Unbreakable" line. */
    HIDE_UNBREAKABLE("HIDE_UNBREAKABLE"),
    /** Hides "Can destroy" lines. */
    HIDE_DESTROYS("HIDE_DESTROYS"),
    /** Hides "Can be placed on" lines. */
    HIDE_PLACED_ON("HIDE_PLACED_ON"),
    /** Hides potion effects and other additional tooltip lines (1.8 HIDE_POTION_EFFECTS, modern HIDE_ADDITIONAL_TOOLTIP). */
    HIDE_ADDITIONAL_TOOLTIP("HIDE_ADDITIONAL_TOOLTIP", "HIDE_POTION_EFFECTS"),
    /** Hides leather armour dye colour (modern only). */
    HIDE_DYE("HIDE_DYE"),
    /** Hides armour trims (modern only). */
    HIDE_ARMOR_TRIM("HIDE_ARMOR_TRIM"),
    /** Hides stored enchantments on enchanted books (modern only). */
    HIDE_STORED_ENCHANTS("HIDE_STORED_ENCHANTS");

    private final List<String> bukkitNames;

    ItemFlagKey(String... bukkitNames) {
        this.bukkitNames = Collections.unmodifiableList(Arrays.asList(bukkitNames));
    }

    /**
     * Returns the Bukkit {@code ItemFlag} names that implement this flag, newest first.
     *
     * @return candidate constant names
     */
    public List<String> bukkitNames() {
        return bukkitNames;
    }
}
