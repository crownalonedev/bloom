package io.github.crownalonedev.bloom.platform.legacy;

import io.github.crownalonedev.bloom.bukkit.platform.PlayerPlatform;
import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import io.github.crownalonedev.bloom.common.text.LegacyText;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Player/inventory operations for 1.7.10 - 1.8.x. */
final class LegacyPlayers implements PlayerPlatform {

    /** Inventory titles longer than this make 1.7/1.8 servers throw. */
    static final int MAX_TITLE_LENGTH = 32;

    private final Method getOnlinePlayers;
    private final Logger logger;
    private final AtomicBoolean warnedTitle = new AtomicBoolean();

    LegacyPlayers(Logger logger) {
        this.logger = logger;
        try {
            // Returns Player[] on 1.7.10 and Collection<? extends Player> on 1.8+: resolve reflectively so the call
            // links on both.
            this.getOnlinePlayers = Bukkit.class.getMethod("getOnlinePlayers");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Bukkit.getOnlinePlayers() not found", e);
        }
    }

    @Override
    public Collection<? extends Player> onlinePlayers() {
        try {
            return adapt(getOnlinePlayers.invoke(null));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not list online players", e);
        }
    }

    /**
     * Normalises the result of {@code Bukkit.getOnlinePlayers()} across versions.
     *
     * @param result {@code Player[]} (1.7.10) or a collection (1.8+)
     * @return an unmodifiable snapshot
     */
    @SuppressWarnings("unchecked")
    static Collection<? extends Player> adapt(Object result) {
        if (result instanceof Player[]) {
            return Collections.unmodifiableList(Arrays.asList((Player[]) result));
        }
        if (result instanceof Collection) {
            return Collections.unmodifiableList(new ArrayList<Player>((Collection<? extends Player>) result));
        }
        throw new IllegalStateException("Unexpected getOnlinePlayers() result: " + result);
    }

    @Override
    public Inventory openTopInventory(Player player) {
        return player.getOpenInventory().getTopInventory();
    }

    @Override
    public InventoryHolder holder(Inventory inventory) {
        return inventory.getHolder();
    }

    @Override
    public Inventory createInventory(InventoryHolder holder, int size, FormattedText title) {
        return Bukkit.createInventory(holder, size, fitTitle(LegacyTextPlatform.render(title)));
    }

    String fitTitle(String title) {
        if (title.length() <= MAX_TITLE_LENGTH) {
            return title;
        }
        if (warnedTitle.compareAndSet(false, true)) {
            logger.warning("Inventory title '" + LegacyText.strip(title) + "' is longer than " + MAX_TITLE_LENGTH
                    + " characters (including colour codes), which 1.7/1.8 servers do not allow; it was truncated. "
                    + "(This warning is shown once.)");
        }
        return LegacyText.truncate(title, MAX_TITLE_LENGTH);
    }
}
