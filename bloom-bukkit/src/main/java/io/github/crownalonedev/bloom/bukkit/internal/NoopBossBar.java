package io.github.crownalonedev.bloom.bukkit.internal;

import io.github.crownalonedev.bloom.bukkit.text.BloomBossBar;
import io.github.crownalonedev.bloom.bukkit.text.BossBarColor;
import io.github.crownalonedev.bloom.bukkit.text.BossBarStyle;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.bukkit.entity.Player;

/** Boss bar returned on platforms without boss bars when the policy is not {@code THROW}. Renders nothing. */
public final class NoopBossBar implements BloomBossBar {

    private boolean closed;

    @Override
    public BloomBossBar title(String title) {
        return this;
    }

    @Override
    public BloomBossBar progress(float progress) {
        BossBars.checkProgress(progress);
        return this;
    }

    @Override
    public BloomBossBar color(BossBarColor color) {
        return this;
    }

    @Override
    public BloomBossBar style(BossBarStyle style) {
        return this;
    }

    @Override
    public BloomBossBar show(Player player) {
        return this;
    }

    @Override
    public BloomBossBar hide(Player player) {
        return this;
    }

    @Override
    public Collection<UUID> viewers() {
        return Collections.emptySet();
    }

    @Override
    public boolean isFunctional() {
        return false;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
