package io.github.crownalonedev.bloom.gui;

import io.github.crownalonedev.bloom.api.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * One player's instance of an open {@link Menu}: the current page, per-player {@link MenuState} and the rendered
 * inventory. Main thread only.
 *
 * @since 0.1.0
 */
public final class MenuView {

    private final Menus menus;
    private final Player player;
    private final Menu menu;
    private final MenuState state = new MenuState();
    private int page;
    private int pageCount = 1;
    private Inventory inventory;
    private MenuHolder holder;
    private String renderedTitle;
    private Button[] slotButtons;
    private ScheduledTask refreshTask;
    private boolean open;

    MenuView(Menus menus, Player player, Menu menu) {
        this.menus = menus;
        this.player = player;
        this.menu = menu;
    }

    /**
     * Returns the viewer.
     *
     * @return player
     */
    public Player player() {
        return player;
    }

    /**
     * Returns the menu definition.
     *
     * @return menu
     */
    public Menu menu() {
        return menu;
    }

    /**
     * Returns this view's state.
     *
     * @return state
     */
    public MenuState state() {
        return state;
    }

    /**
     * Returns the zero-based current page.
     *
     * @return page
     */
    public int page() {
        return page;
    }

    /**
     * Returns the number of pages computed at the last render.
     *
     * @return page count, at least 1
     */
    public int pageCount() {
        return pageCount;
    }

    /**
     * Returns whether a next page exists.
     *
     * @return {@code true} if {@code page() < pageCount() - 1}
     */
    public boolean hasNextPage() {
        return page < pageCount - 1;
    }

    /**
     * Returns whether a previous page exists.
     *
     * @return {@code true} if {@code page() > 0}
     */
    public boolean hasPreviousPage() {
        return page > 0;
    }

    /**
     * Switches to a page (clamped to the valid range) and re-renders.
     *
     * @param newPage zero-based page
     */
    public void page(int newPage) {
        this.page = Math.max(0, Math.min(newPage, pageCount - 1));
        refresh();
    }

    /** Switches to the next page if there is one. */
    public void nextPage() {
        if (hasNextPage()) {
            page(page + 1);
        }
    }

    /** Switches to the previous page if there is one. */
    public void previousPage() {
        if (hasPreviousPage()) {
            page(page - 1);
        }
    }

    /**
     * Re-renders the menu for this viewer. Items are updated in place; if the rendered title changed (for example a
     * page number in the title) the inventory is reopened on the next tick, keeping navigation history.
     */
    public void refresh() {
        menus.refreshView(this);
    }

    /**
     * Returns whether this view is currently shown to the player.
     *
     * @return {@code true} while open
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Returns the rendered inventory.
     *
     * @return inventory, or {@code null} before the first render
     */
    public Inventory inventory() {
        return inventory;
    }

    // ---- package-private state used by Menus ----

    void setPageUnchecked(int newPage) {
        this.page = Math.max(0, newPage);
    }

    void pageCount(int count) {
        this.pageCount = Math.max(1, count);
        if (page > pageCount - 1) {
            page = pageCount - 1;
        }
    }

    MenuHolder holder() {
        return holder;
    }

    void attach(MenuHolder newHolder, Inventory newInventory, String title) {
        this.holder = newHolder;
        this.inventory = newInventory;
        this.renderedTitle = title;
    }

    String renderedTitle() {
        return renderedTitle;
    }

    Button[] slotButtons() {
        return slotButtons;
    }

    void slotButtons(Button[] buttons) {
        this.slotButtons = buttons;
    }

    ScheduledTask refreshTask() {
        return refreshTask;
    }

    void refreshTask(ScheduledTask task) {
        this.refreshTask = task;
    }

    void open(boolean value) {
        this.open = value;
    }

    Menus menus() {
        return menus;
    }
}
