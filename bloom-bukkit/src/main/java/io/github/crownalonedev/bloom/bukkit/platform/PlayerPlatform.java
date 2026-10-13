package io.github.crownalonedev.bloom.bukkit.platform;

import java.util.Collection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Adapter SPI for player and inventory operations whose binary signatures differ between server versions.
 *
 * <p>Examples: {@code Bukkit.getOnlinePlayers()} returns an array on 1.7.10 and a collection on 1.8+;
 * {@code InventoryView} is a class before 1.21 and an interface after, so calling its methods from shared code would
 * fail with {@code IncompatibleClassChangeError}.
 *
 * @since 0.1.0
 */
public interface PlayerPlatform {

    /**
     * Returns the online players.
     *
     * @return snapshot of online players
     */
    Collection<? extends Player> onlinePlayers();

    /**
     * Returns the top inventory of the player's open inventory view.
     *
     * @param player player
     * @return top inventory (the player's crafting inventory when no container is open)
     */
    Inventory openTopInventory(Player player);

    /**
     * Returns an inventory's holder as cheaply as possible. On Paper this avoids creating block-state snapshots,
     * which matters because menu listeners inspect every inventory click on the server.
     *
     * @param inventory inventory
     * @return holder, or {@code null}
     */
    InventoryHolder holder(Inventory inventory);

    /**
     * Creates a chest inventory.
     *
     * @param holder holder identifying the inventory
     * @param size slot count, a multiple of 9 between 9 and 54
     * @param title prepared title; legacy adapters truncate to 32 characters
     * @return the inventory
     */
    Inventory createInventory(
            InventoryHolder holder, int size, io.github.crownalonedev.bloom.bukkit.text.FormattedText title);
}
