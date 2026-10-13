package io.github.crownalonedev.bloom.gui;

import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.inventory.ItemStack;

/**
 * Builds yes/no confirmation dialogs.
 *
 * <pre>{@code
 * Menu confirm = ConfirmMenu.builder()
 *         .title("&cDelete home '{home}'?")
 *         .confirm(items.builder("LIME_WOOL").name("&aDelete").build(), click -> {
 *             homes.delete(click.player(), name);
 *             click.close();
 *         })
 *         .cancel(items.builder("RED_WOOL").name("&cKeep").build())
 *         .build();
 * click.open(confirm);
 * }</pre>
 *
 * <p>The default cancel action goes back to the previous menu, or closes the dialog if there is none. Clicking
 * confirm repeatedly is guarded by the menu click debounce.
 *
 * @since 0.1.0
 */
public final class ConfirmMenu {

    private String title = "Are you sure?";
    private ItemStack confirmItem;
    private Consumer<ClickContext> onConfirm;
    private ItemStack cancelItem;
    private Consumer<ClickContext> onCancel = new Consumer<ClickContext>() {
        @Override
        public void accept(ClickContext click) {
            if (!click.back()) {
                click.close();
            }
        }
    };
    private Button filler;
    private ItemStack info;

    private ConfirmMenu() {}

    /**
     * Starts a dialog.
     *
     * @return builder
     */
    public static ConfirmMenu builder() {
        return new ConfirmMenu();
    }

    /**
     * Sets the title.
     *
     * @param template title template
     * @return this builder
     */
    public ConfirmMenu title(String template) {
        this.title = Objects.requireNonNull(template, "title");
        return this;
    }

    /**
     * Sets the confirm button.
     *
     * @param item confirm item
     * @param action runs when confirmed
     * @return this builder
     */
    public ConfirmMenu confirm(ItemStack item, Consumer<ClickContext> action) {
        this.confirmItem = Objects.requireNonNull(item, "item");
        this.onConfirm = Objects.requireNonNull(action, "action");
        return this;
    }

    /**
     * Sets the cancel item with the default action (back, or close).
     *
     * @param item cancel item
     * @return this builder
     */
    public ConfirmMenu cancel(ItemStack item) {
        this.cancelItem = Objects.requireNonNull(item, "item");
        return this;
    }

    /**
     * Sets the cancel button.
     *
     * @param item cancel item
     * @param action runs when cancelled
     * @return this builder
     */
    public ConfirmMenu cancel(ItemStack item, Consumer<ClickContext> action) {
        this.cancelItem = Objects.requireNonNull(item, "item");
        this.onCancel = Objects.requireNonNull(action, "action");
        return this;
    }

    /**
     * Shows an informational item in the centre.
     *
     * @param item info item
     * @return this builder
     */
    public ConfirmMenu info(ItemStack item) {
        this.info = item;
        return this;
    }

    /**
     * Fills empty slots.
     *
     * @param button filler
     * @return this builder
     */
    public ConfirmMenu fill(Button button) {
        this.filler = button;
        return this;
    }

    /**
     * Builds the dialog as a 3-row menu (confirm at slot 11, info at 13, cancel at 15).
     *
     * @return menu
     */
    public Menu build() {
        if (confirmItem == null || cancelItem == null) {
            throw new IllegalStateException("A confirmation dialog needs both confirm(...) and cancel(...).");
        }
        Menu.Builder builder = Menu.builder()
                .title(title)
                .rows(3)
                .button(11, Button.of(confirmItem, onConfirm))
                .button(15, Button.of(cancelItem, onCancel));
        if (info != null) {
            builder.item(13, info);
        }
        if (filler != null) {
            builder.fill(filler);
        }
        return builder.build();
    }
}
