package io.github.crownalonedev.bloom.gui;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.inventory.ItemStack;

/**
 * An item in a menu slot with an optional click handler. Buttons are immutable; the {@code with*}/{@code on*}
 * methods return modified copies, so one button can be reused in many menus.
 *
 * <pre>{@code
 * Button heal = Button.of(items.builder("GOLDEN_APPLE").name("&aHeal").build())
 *         .permission("shop.heal")
 *         .onClick(click -> {
 *             click.player().setHealth(20.0);
 *             click.close();
 *         });
 *
 * Button counter = Button.dynamic(view -> items.builder("PAPER")
 *         .name("&fClicks: " + view.state().getOrDefault(CLICKS, 0)).build())
 *         .onClick(click -> {
 *             click.view().state().set(CLICKS, click.view().state().getOrDefault(CLICKS, 0) + 1);
 *             click.refresh();
 *         });
 * }</pre>
 *
 * @since 0.1.0
 */
public final class Button {

    private static final Consumer<ClickContext> NO_OP = new Consumer<ClickContext>() {
        @Override
        public void accept(ClickContext click) {}
    };

    private final Function<MenuView, ItemStack> icon;
    private final Consumer<ClickContext> handler;
    private final Predicate<MenuView> visible;
    private final String permission;
    private final ItemStack deniedIcon;

    private Button(
            Function<MenuView, ItemStack> icon,
            Consumer<ClickContext> handler,
            Predicate<MenuView> visible,
            String permission,
            ItemStack deniedIcon) {
        this.icon = icon;
        this.handler = handler;
        this.visible = visible;
        this.permission = permission;
        this.deniedIcon = deniedIcon;
    }

    /**
     * Creates a button with a fixed item and no click action (a decoration).
     *
     * @param item item to display (copied when rendered)
     * @return the button
     */
    public static Button of(ItemStack item) {
        final ItemStack copy = Objects.requireNonNull(item, "item").clone();
        return new Button(view -> copy, NO_OP, null, null, null);
    }

    /**
     * Creates a button with a fixed item and a click action.
     *
     * @param item item to display
     * @param onClick click handler, runs on the main thread
     * @return the button
     */
    public static Button of(ItemStack item, Consumer<ClickContext> onClick) {
        return of(item).onClick(onClick);
    }

    /**
     * Creates a button whose item is computed for each viewer on every render (open, refresh, page change).
     *
     * @param icon computes the item; may return {@code null} for an empty slot
     * @return the button
     */
    public static Button dynamic(Function<MenuView, ItemStack> icon) {
        return new Button(Objects.requireNonNull(icon, "icon"), NO_OP, null, null, null);
    }

    /**
     * Returns a copy with a click handler. Handlers run on the main thread; exceptions are logged and do not break
     * the menu.
     *
     * @param onClick click handler
     * @return a new button
     */
    public Button onClick(Consumer<ClickContext> onClick) {
        return new Button(icon, Objects.requireNonNull(onClick, "onClick"), visible, permission, deniedIcon);
    }

    /**
     * Returns a copy that is only shown when a condition holds (evaluated on each render). Hidden buttons leave the
     * slot to the menu's filler.
     *
     * @param condition visibility condition
     * @return a new button
     */
    public Button visibleWhen(Predicate<MenuView> condition) {
        Predicate<MenuView> combined = visible == null ? condition : visible.and(condition);
        return new Button(icon, handler, combined, permission, deniedIcon);
    }

    /**
     * Returns a copy that is hidden from players without a permission.
     *
     * @param node permission node
     * @return a new button
     */
    public Button permission(String node) {
        return new Button(icon, handler, visible, Objects.requireNonNull(node, "node"), null);
    }

    /**
     * Returns a copy that shows {@code deniedItem} (without running the click handler) to players lacking a
     * permission.
     *
     * @param node permission node
     * @param deniedItem item shown instead
     * @return a new button
     */
    public Button permission(String node, ItemStack deniedItem) {
        return new Button(icon, handler, visible, Objects.requireNonNull(node, "node"), deniedItem.clone());
    }

    /** Result of resolving a button for one viewer. */
    static final class Resolved {
        final ItemStack item;
        final boolean clickable;

        Resolved(ItemStack item, boolean clickable) {
            this.item = item;
            this.clickable = clickable;
        }
    }

    /**
     * Resolves the button for a view.
     *
     * @return the resolved slot, or {@code null} if the button is hidden
     */
    Resolved resolve(MenuView view) {
        if (visible != null && !visible.test(view)) {
            return null;
        }
        if (permission != null && !view.player().hasPermission(permission)) {
            return deniedIcon == null ? null : new Resolved(deniedIcon.clone(), false);
        }
        ItemStack item = icon.apply(view);
        return new Resolved(item == null ? null : item.clone(), true);
    }

    void click(ClickContext context) {
        handler.accept(context);
    }

    boolean hasHandler() {
        return handler != NO_OP;
    }
}
