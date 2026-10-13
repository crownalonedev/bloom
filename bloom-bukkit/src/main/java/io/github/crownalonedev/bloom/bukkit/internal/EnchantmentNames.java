package io.github.crownalonedev.bloom.bukkit.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps between legacy Bukkit enchantment names ({@code DAMAGE_ALL}) and modern Minecraft keys ({@code sharpness}).
 * Covers every enchantment that exists on 1.8.
 */
public final class EnchantmentNames {

    private static final Map<String, String> LEGACY_TO_KEY;
    private static final Map<String, String> KEY_TO_LEGACY;

    static {
        String[][] pairs = {
            {"PROTECTION_ENVIRONMENTAL", "protection"},
            {"PROTECTION_FIRE", "fire_protection"},
            {"PROTECTION_FALL", "feather_falling"},
            {"PROTECTION_EXPLOSIONS", "blast_protection"},
            {"PROTECTION_PROJECTILE", "projectile_protection"},
            {"OXYGEN", "respiration"},
            {"WATER_WORKER", "aqua_affinity"},
            {"THORNS", "thorns"},
            {"DEPTH_STRIDER", "depth_strider"},
            {"DAMAGE_ALL", "sharpness"},
            {"DAMAGE_UNDEAD", "smite"},
            {"DAMAGE_ARTHROPODS", "bane_of_arthropods"},
            {"KNOCKBACK", "knockback"},
            {"FIRE_ASPECT", "fire_aspect"},
            {"LOOT_BONUS_MOBS", "looting"},
            {"DIG_SPEED", "efficiency"},
            {"SILK_TOUCH", "silk_touch"},
            {"DURABILITY", "unbreaking"},
            {"LOOT_BONUS_BLOCKS", "fortune"},
            {"ARROW_DAMAGE", "power"},
            {"ARROW_KNOCKBACK", "punch"},
            {"ARROW_FIRE", "flame"},
            {"ARROW_INFINITE", "infinity"},
            {"LUCK", "luck_of_the_sea"},
            {"LURE", "lure"},
        };
        Map<String, String> toKey = new HashMap<String, String>();
        Map<String, String> toLegacy = new HashMap<String, String>();
        for (String[] pair : pairs) {
            toKey.put(pair[0], pair[1]);
            toLegacy.put(pair[1], pair[0]);
        }
        LEGACY_TO_KEY = Collections.unmodifiableMap(toKey);
        KEY_TO_LEGACY = Collections.unmodifiableMap(toLegacy);
    }

    private EnchantmentNames() {}

    /**
     * Normalises any accepted spelling to a modern key.
     *
     * @param name {@code "sharpness"}, {@code "minecraft:sharpness"}, {@code "DAMAGE_ALL"} or {@code "SHARPNESS"}
     * @return modern key without namespace, e.g. {@code "sharpness"}
     */
    public static String toKey(String name) {
        String trimmed = name.trim();
        String legacyMapped = LEGACY_TO_KEY.get(trimmed.toUpperCase(Locale.ROOT));
        if (legacyMapped != null) {
            return legacyMapped;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.startsWith("minecraft:") ? lower.substring("minecraft:".length()) : lower;
    }

    /**
     * Converts any accepted spelling to the legacy Bukkit name.
     *
     * @param name enchantment key or name
     * @return legacy name, e.g. {@code "DAMAGE_ALL"}; for enchantments newer than 1.8 the upper-cased key
     */
    public static String toLegacy(String name) {
        String key = toKey(name);
        String legacy = KEY_TO_LEGACY.get(key);
        return legacy != null ? legacy : key.toUpperCase(Locale.ROOT);
    }
}
