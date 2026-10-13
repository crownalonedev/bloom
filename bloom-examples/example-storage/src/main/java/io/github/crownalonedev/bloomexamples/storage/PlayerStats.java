package io.github.crownalonedev.bloomexamples.storage;

import java.util.UUID;

/** Immutable statistics row. */
public final class PlayerStats {

    private final UUID uuid;
    private final int kills;
    private final int deaths;

    public PlayerStats(UUID uuid, int kills, int deaths) {
        this.uuid = uuid;
        this.kills = kills;
        this.deaths = deaths;
    }

    public UUID uuid() {
        return uuid;
    }

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }
}
