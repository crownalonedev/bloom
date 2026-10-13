package io.github.crownalonedev.bloomexamples.docs;

import io.github.crownalonedev.bloom.api.util.Cooldowns;
import io.github.crownalonedev.bloom.api.util.Durations;
import io.github.crownalonedev.bloom.api.util.ExpiringCache;
import java.time.Duration;
import java.util.UUID;
import org.bukkit.entity.Player;

/** Utility snippets used in the documentation. */
final class UtilitySnippets {

    private UtilitySnippets() {}

    // @snippet:utilities.cooldowns
    private final Cooldowns<UUID> enderPearl = Cooldowns.of(Duration.ofSeconds(16));

    boolean tryThrowPearl(Player player) {
        if (!enderPearl.tryUse(player.getUniqueId())) {
            Duration left = enderPearl.remaining(player.getUniqueId());
            player.sendMessage("Pearl cooldown: " + Durations.format(left)); // "Pearl cooldown: 12s"
            return false;
        }
        return true;
    }
    // @end

    static void durations() {
        // @snippet:utilities.durations
        Duration banLength = Durations.parse("1d 12h"); // also "90s", "1h30m", "500ms", "2w"
        String shortForm = Durations.format(banLength); // "1d 12h"
        String words = Durations.formatLong(banLength); // "1 day and 12 hours"
        // @end
        shortForm.hashCode();
        words.hashCode();
    }

    static void cache() {
        // @snippet:utilities.cache
        ExpiringCache<UUID, String> ranks = ExpiringCache.create(Duration.ofMinutes(5), 1_000);
        UUID id = UUID.randomUUID();
        String rank = ranks.get(id, key -> "member"); // computes and caches on a miss
        ranks.invalidate(id);
        // @end
        rank.hashCode();
    }
}
