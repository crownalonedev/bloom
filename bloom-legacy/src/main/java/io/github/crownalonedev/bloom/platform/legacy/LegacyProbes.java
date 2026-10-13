package io.github.crownalonedev.bloom.platform.legacy;

import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Feature probes that use reflection only, so they can run on any legacy server. The {@code *V18} classes that call
 * the probed APIs directly are loaded only after the corresponding probe succeeds.
 */
final class LegacyProbes {

    private LegacyProbes() {}

    /** {@code Player#sendTitle(String, String)} — Spigot 1.8.x. */
    static boolean titles() {
        try {
            Player.class.getMethod("sendTitle", String.class, String.class);
            return true;
        } catch (NoSuchMethodException | LinkageError e) {
            return false;
        }
    }

    /** {@code ItemFlag} and {@code ItemMeta#addItemFlags} — 1.8+. */
    static boolean itemFlags() {
        try {
            ItemMeta.class.getMethod("addItemFlags", Class.forName("[Lorg.bukkit.inventory.ItemFlag;"));
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
            return false;
        }
    }

    /** {@code ItemMeta.spigot().setUnbreakable(boolean)} — Spigot. */
    static boolean unbreakable() {
        try {
            ItemMeta.class.getMethod("spigot");
            Class.forName("org.bukkit.inventory.meta.ItemMeta$Spigot").getMethod("setUnbreakable", boolean.class);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError e) {
            return false;
        }
    }

    /** Whether a class exists without initialising it. */
    static boolean classExists(String name) {
        try {
            Class.forName(name, false, LegacyProbes.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
