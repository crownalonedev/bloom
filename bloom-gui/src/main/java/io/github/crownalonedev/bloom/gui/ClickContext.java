package io.github.crownalonedev.bloom.gui;

import org.bukkit.entity.Player;

/**
 * Information about a button click, passed to {@link Button#onClick} handlers.
 *
 * <p>The underlying Bukkit event is always cancelled; items never move in or out of a Bloom menu. Navigation methods
 * ({@link #open(Menu)}, {@link #back()}, {@link #close()}) take effect on the next tick, because opening or closing
 * inventories inside a click event is unsafe on Bukkit servers.
 *
 * @since 0.1.0
 */
public final class ClickContext {

    private final MenuView view;
    private final int slot;
    private final ClickKind kind;
    private final int hotbarButton;

    ClickContext(MenuView view, int slot, ClickKind kind, int hotbarButton) {
        this.view = view;
        this.slot = slot;
        this.kind = kind;
        this.hotbarButton = hotbarButton;
    }

    /**
     * Returns the clicking player.
     *
     * @return player
     */
    public Player player() {
        return view.player();
    }

    /**
     * Returns the player's view of the menu (page, state).
     *
     * @return view
     */
    public MenuView view() {
        return view;
    }

    /**
     * Returns the menu definition.
     *
     * @return menu
     */
    public Menu menu() {
        return view.menu();
    }

    /**
     * Returns the clicked slot.
     *
     * @return slot index in the menu
     */
    public int slot() {
        return slot;
    }

    /**
     * Returns the kind of click.
     *
     * @return click kind
     */
    public ClickKind kind() {
        return kind;
    }

    /**
     * Returns the number key pressed for {@link ClickKind#NUMBER_KEY} clicks.
     *
     * @return hotbar slot 0-8, or -1
     */
    public int hotbarButton() {
        return hotbarButton;
    }

    /**
     * Returns the menu service.
     *
     * @return menus
     */
    public Menus menus() {
        return view.menus();
    }

    /** Re-renders the current menu for this player. */
    public void refresh() {
        view.refresh();
    }

    /**
     * Opens another menu on the next tick. The current menu is kept in the history for {@link #back()}.
     *
     * @param menu menu to open
     */
    public void open(final Menu menu) {
        view.menus().openLater(view.player(), menu);
    }

    /**
     * Goes back to the previous menu on the next tick.
     *
     * @return {@code true} if there is a previous menu
     */
    public boolean back() {
        return view.menus().backLater(view.player());
    }

    /** Closes the menu on the next tick. */
    public void close() {
        view.menus().closeLater(view.player());
    }
}
