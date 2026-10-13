package io.github.crownalonedev.bloom.bukkit.platform;

import io.github.crownalonedev.bloom.bukkit.text.BloomBossBar;
import io.github.crownalonedev.bloom.bukkit.text.BossBarColor;
import io.github.crownalonedev.bloom.bukkit.text.BossBarStyle;
import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import java.time.Duration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Adapter SPI for rendering and delivering text. Implemented by each platform adapter.
 *
 * <p><strong>SPI:</strong> plugin code should use {@link io.github.crownalonedev.bloom.bukkit.text.Messenger}.
 * Methods guarded by a capability are only called by Bloom when that capability is present.
 *
 * @since 0.1.0
 */
public interface TextPlatform {

    /**
     * Sends a chat message.
     *
     * @param target recipient (player or console)
     * @param text prepared text
     */
    void sendMessage(CommandSender target, FormattedText text);

    /**
     * Sends an action bar message. Requires {@code ACTION_BAR}.
     *
     * @param player recipient
     * @param text prepared text
     */
    void sendActionBar(Player player, FormattedText text);

    /**
     * Shows a title. Requires {@code TITLES}; timings are honoured only with {@code TITLE_TIMINGS}.
     *
     * @param player recipient
     * @param title prepared title
     * @param subtitle prepared subtitle
     * @param fadeIn fade-in
     * @param stay stay
     * @param fadeOut fade-out
     */
    void sendTitle(
            Player player,
            FormattedText title,
            FormattedText subtitle,
            Duration fadeIn,
            Duration stay,
            Duration fadeOut);

    /**
     * Creates a boss bar. Requires {@code BOSS_BAR}.
     *
     * @param title prepared title
     * @param progress progress {@code 0..1}
     * @param color colour
     * @param style style
     * @param renderer renders later title updates given as templates in the messenger's format
     * @return a functional boss bar
     */
    BloomBossBar createBossBar(
            FormattedText title, float progress, BossBarColor color, BossBarStyle style, TitleRenderer renderer);

    /**
     * Renders text to a legacy {@code §} string, e.g. for APIs that only accept strings.
     *
     * @param text prepared text
     * @return legacy formatted string
     */
    String toLegacyString(FormattedText text);

    /** Converts a title template into prepared text using the owning messenger's settings. */
    interface TitleRenderer {
        /**
         * Prepares a template.
         *
         * @param template template text
         * @return prepared text
         */
        FormattedText render(String template);
    }
}
