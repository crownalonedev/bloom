package io.github.crownalonedev.bloom.platform.legacy;

import org.bukkit.inventory.meta.ItemMeta;

/**
 * Unbreakable items through Spigot's {@code ItemMeta.spigot().setUnbreakable}. Only loaded after
 * the matching {@link LegacyProbes} check passed.
 */
final class UnbreakableV18 {

    private UnbreakableV18() {}

    static void set(ItemMeta meta, boolean unbreakable) {
        meta.spigot().setUnbreakable(unbreakable);
    }
}
