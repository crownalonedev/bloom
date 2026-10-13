package io.github.crownalonedev.bloom.platform.modern;

import io.github.crownalonedev.bloom.api.text.TextFormat;
import io.github.crownalonedev.bloom.bukkit.internal.BossBars;
import io.github.crownalonedev.bloom.bukkit.platform.TextPlatform;
import io.github.crownalonedev.bloom.bukkit.text.BloomBossBar;
import io.github.crownalonedev.bloom.bukkit.text.BossBarColor;
import io.github.crownalonedev.bloom.bukkit.text.BossBarStyle;
import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Text rendering through Adventure. */
final class ModernText implements TextPlatform {

    /** Parses {@code §} strings including Spigot-style {@code §x§R§R§G§G§B§B} hex colours. */
    static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    Component component(FormattedText text) {
        if (text.isEmpty()) {
            return Component.empty();
        }
        return text.format() == TextFormat.MINIMESSAGE
                ? miniMessage.deserialize(text.content())
                : SECTION.deserialize(text.content());
    }

    /**
     * Item names and lore render white and non-italic unless the text sets its own colour/italics, matching the
     * appearance of legacy servers.
     */
    Component itemComponent(FormattedText text) {
        return component(text)
                .colorIfAbsent(NamedTextColor.WHITE)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    @Override
    public void sendMessage(CommandSender target, FormattedText text) {
        target.sendMessage(component(text));
    }

    @Override
    public void sendActionBar(Player player, FormattedText text) {
        player.sendActionBar(component(text));
    }

    @Override
    public void sendTitle(
            Player player,
            FormattedText title,
            FormattedText subtitle,
            Duration fadeIn,
            Duration stay,
            Duration fadeOut) {
        player.showTitle(Title.title(component(title), component(subtitle), Title.Times.times(fadeIn, stay, fadeOut)));
    }

    @Override
    public BloomBossBar createBossBar(
            FormattedText title, float progress, BossBarColor color, BossBarStyle style, TitleRenderer renderer) {
        BossBar bar = BossBar.bossBar(component(title), progress, color(color), overlay(style));
        return new AdventureBossBar(bar, renderer);
    }

    @Override
    public String toLegacyString(FormattedText text) {
        return text.format() == TextFormat.LEGACY ? text.content() : SECTION.serialize(component(text));
    }

    static BossBar.Color color(BossBarColor color) {
        return BossBar.Color.valueOf(color.name());
    }

    static BossBar.Overlay overlay(BossBarStyle style) {
        switch (style) {
            case SEGMENTED_6:
                return BossBar.Overlay.NOTCHED_6;
            case SEGMENTED_10:
                return BossBar.Overlay.NOTCHED_10;
            case SEGMENTED_12:
                return BossBar.Overlay.NOTCHED_12;
            case SEGMENTED_20:
                return BossBar.Overlay.NOTCHED_20;
            case SOLID:
            default:
                return BossBar.Overlay.PROGRESS;
        }
    }

    private final class AdventureBossBar implements BloomBossBar {
        private final BossBar bar;
        private final TitleRenderer renderer;
        private final Set<UUID> viewers = new LinkedHashSet<>();
        private boolean closed;

        AdventureBossBar(BossBar bar, TitleRenderer renderer) {
            this.bar = bar;
            this.renderer = renderer;
        }

        @Override
        public BloomBossBar title(String title) {
            bar.name(component(renderer.render(title)));
            return this;
        }

        @Override
        public BloomBossBar progress(float progress) {
            bar.progress(BossBars.checkProgress(progress));
            return this;
        }

        @Override
        public BloomBossBar color(BossBarColor color) {
            bar.color(ModernText.color(color));
            return this;
        }

        @Override
        public BloomBossBar style(BossBarStyle style) {
            bar.overlay(overlay(style));
            return this;
        }

        @Override
        public BloomBossBar show(Player player) {
            if (!closed && viewers.add(player.getUniqueId())) {
                player.showBossBar(bar);
            }
            return this;
        }

        @Override
        public BloomBossBar hide(Player player) {
            if (viewers.remove(player.getUniqueId())) {
                player.hideBossBar(bar);
            }
            return this;
        }

        @Override
        public Collection<UUID> viewers() {
            return new ArrayList<>(viewers);
        }

        @Override
        public boolean isFunctional() {
            return true;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            List<UUID> snapshot = new ArrayList<>(viewers);
            viewers.clear();
            for (UUID id : snapshot) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    player.hideBossBar(bar);
                }
            }
        }

        @Override
        public boolean isClosed() {
            return closed;
        }
    }
}
