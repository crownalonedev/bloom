package io.github.crownalonedev.bloom.platform.modern;

import io.github.crownalonedev.bloom.bukkit.platform.PlayerPlatform;
import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Player and inventory operations compiled against the modern API (InventoryView is an interface here). */
final class ModernPlayers implements PlayerPlatform {

    private final ModernText text;
    private final boolean snapshotFreeHolders;

    /**
     * Creates the platform.
     *
     * @param text text rendering
     * @param snapshotFreeHolders whether {@code Inventory#getHolder(false)} may be used; MockBukkit does not
     *     implement it, so it is disabled there
     */
    ModernPlayers(ModernText text, boolean snapshotFreeHolders) {
        this.text = text;
        this.snapshotFreeHolders = snapshotFreeHolders;
    }

    @Override
    public Collection<? extends Player> onlinePlayers() {
        return Bukkit.getOnlinePlayers();
    }

    @Override
    public Inventory openTopInventory(Player player) {
        return player.getOpenInventory().getTopInventory();
    }

    @Override
    public InventoryHolder holder(Inventory inventory) {
        return snapshotFreeHolders ? inventory.getHolder(false) : inventory.getHolder();
    }

    @Override
    public Inventory createInventory(InventoryHolder holder, int size, FormattedText title) {
        return Bukkit.createInventory(holder, size, text.component(title));
    }
}
