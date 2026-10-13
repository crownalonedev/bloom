package io.github.crownalonedev.bloom.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Identifies Bloom menu inventories. Using a custom holder (instead of titles) works identically from 1.7.10 to
 * 26.x and cannot be spoofed by renaming items or containers.
 */
final class MenuHolder implements InventoryHolder {

    private final MenuView view;
    private Inventory inventory;

    MenuHolder(MenuView view) {
        this.view = view;
    }

    MenuView view() {
        return view;
    }

    void inventory(Inventory created) {
        this.inventory = created;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
