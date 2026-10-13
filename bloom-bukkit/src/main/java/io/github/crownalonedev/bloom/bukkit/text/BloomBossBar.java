package io.github.crownalonedev.bloom.bukkit.text;

import io.github.crownalonedev.bloom.api.lifecycle.Terminable;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * A boss bar shown to a set of players. Created by {@link Messenger#bossBar}.
 *
 * <p>Must be used from the main thread. Closing the bar hides it from all viewers; Bloom closes open bars on
 * shutdown. On platforms without {@code Capability.BOSS_BAR} (with the {@code SKIP} policy) a no-op bar is
 * returned whose {@link #isFunctional()} is {@code false}.
 *
 * @since 0.1.0
 */
public interface BloomBossBar extends Terminable {

    /**
     * Sets the title.
     *
     * @param title title in the owning messenger's format
     * @return this bar
     */
    BloomBossBar title(String title);

    /**
     * Sets the progress.
     *
     * @param progress value between {@code 0.0} and {@code 1.0}
     * @return this bar
     * @throws IllegalArgumentException if out of range
     */
    BloomBossBar progress(float progress);

    /**
     * Sets the colour.
     *
     * @param color colour
     * @return this bar
     */
    BloomBossBar color(BossBarColor color);

    /**
     * Sets the style.
     *
     * @param style style
     * @return this bar
     */
    BloomBossBar style(BossBarStyle style);

    /**
     * Shows the bar to a player.
     *
     * @param player viewer to add
     * @return this bar
     */
    BloomBossBar show(Player player);

    /**
     * Hides the bar from a player.
     *
     * @param player viewer to remove
     * @return this bar
     */
    BloomBossBar hide(Player player);

    /**
     * Returns the current viewers.
     *
     * @return snapshot of viewer UUIDs
     */
    Collection<UUID> viewers();

    /**
     * Returns whether this bar actually renders on the current platform.
     *
     * @return {@code false} for the no-op fallback bar
     */
    boolean isFunctional();
}
