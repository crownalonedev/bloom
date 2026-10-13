package io.github.crownalonedev.bloom.platform.legacy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

/** Item flags; {@code ItemFlag} exists from 1.8. Only loaded after the matching {@link LegacyProbes} check passed. */
final class ItemFlagsV18 {

    private ItemFlagsV18() {}

    static List<String> add(ItemMeta meta, Collection<String> names) {
        List<String> unknown = new ArrayList<String>();
        for (String name : names) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(name));
            } catch (IllegalArgumentException e) {
                unknown.add(name);
            }
        }
        return unknown;
    }
}
