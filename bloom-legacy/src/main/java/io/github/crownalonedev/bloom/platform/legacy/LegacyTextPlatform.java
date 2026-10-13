package io.github.crownalonedev.bloom.platform.legacy;

import io.github.crownalonedev.bloom.api.text.TextFormat;
import io.github.crownalonedev.bloom.bukkit.platform.TextPlatform;
import io.github.crownalonedev.bloom.bukkit.text.BloomBossBar;
import io.github.crownalonedev.bloom.bukkit.text.BossBarColor;
import io.github.crownalonedev.bloom.bukkit.text.BossBarStyle;
import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import io.github.crownalonedev.bloom.common.text.MiniMessageLite;
import java.time.Duration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Text on legacy servers: everything is rendered to {@code §} strings. */
final class LegacyTextPlatform implements TextPlatform {

    private final LegacyNms nms;

    LegacyTextPlatform(LegacyNms nms) {
        this.nms = nms;
    }

    static String render(FormattedText text) {
        return text.format() == TextFormat.MINIMESSAGE ? MiniMessageLite.toLegacy(text.content()) : text.content();
    }

    @Override
    public void sendMessage(CommandSender target, FormattedText text) {
        target.sendMessage(render(text));
    }

    @Override
    public void sendActionBar(Player player, FormattedText text) {
        nms.sendActionBar(player, render(text));
    }

    @Override
    public void sendTitle(
            Player player,
            FormattedText title,
            FormattedText subtitle,
            Duration fadeIn,
            Duration stay,
            Duration fadeOut) {
        // 1.8 Bukkit API has no timings; the client defaults apply (documented on Title).
        TitlesV18.send(player, render(title), render(subtitle));
    }

    @Override
    public BloomBossBar createBossBar(
            FormattedText title, float progress, BossBarColor color, BossBarStyle style, TitleRenderer renderer) {
        throw new UnsupportedOperationException("Boss bars are not available on legacy servers (capability BOSS_BAR).");
    }

    @Override
    public String toLegacyString(FormattedText text) {
        return render(text);
    }
}
