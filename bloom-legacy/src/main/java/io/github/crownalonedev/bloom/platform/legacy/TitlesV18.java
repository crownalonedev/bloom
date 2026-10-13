package io.github.crownalonedev.bloom.platform.legacy;

import org.bukkit.entity.Player;

/**
 * Calls {@code Player#sendTitle(String, String)}, which exists from Spigot 1.8.x (not on 1.7.10). Only loaded after
 * the matching {@link LegacyProbes} check passed.
 */
final class TitlesV18 {

    private TitlesV18() {}

    static void send(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle);
    }
}
