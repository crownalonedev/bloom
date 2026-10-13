package io.github.crownalonedev.bloom.gui;

import io.github.crownalonedev.bloom.api.text.TextFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.inventory.ItemStack;

/**
 * An immutable chest menu definition. One {@code Menu} can be opened by many players at once; each gets its own
 * {@link MenuView} with its own page and {@link MenuState}.
 *
 * <pre>{@code
 * Menu shop = Menu.builder()
 *         .title("&d&lBlossom Shop")
 *         .rows(3)
 *         .layout(
 *                 "#########",
 *                 "#  a b  #",
 *                 "#########")
 *         .bind('#', Button.of(filler))
 *         .bind('a', healButton)
 *         .bind('b', feedButton)
 *         .build();
 *
 * Menus.of(bloom).open(player, shop);
 * }</pre>
 *
 * <p>Titles may use {@code {player}}, {@code {page}} and {@code {pages}} placeholders. On 1.7/1.8 titles are limited to
 * 32 characters including colour codes and are truncated with a warning.
 *
 * @since 0.1.0
 */
public final class Menu {

    private final String title;
    private final int rows;
    private final TextFormat format;
    private final Map<Integer, Button> buttons;
    private final Button filler;
    private final Function<MenuView, List<Button>> pageSource;
    private final int[] contentSlots;
    private final int previousSlot;
    private final Button previousButton;
    private final int nextSlot;
    private final Button nextButton;
    private final List<Consumer<MenuView>> openHandlers;
    private final List<Consumer<MenuView>> closeHandlers;
    private final long refreshTicks;

    private Menu(Builder b) {
        this.title = b.title;
        this.rows = b.rows;
        this.format = b.format;
        this.buttons = Collections.unmodifiableMap(new HashMap<Integer, Button>(b.buttons));
        this.filler = b.filler;
        this.pageSource = b.pageSource;
        this.contentSlots = b.resolveContentSlots();
        this.previousSlot = b.previousSlot;
        this.previousButton = b.previousButton;
        this.nextSlot = b.nextSlot;
        this.nextButton = b.nextButton;
        this.openHandlers = Collections.unmodifiableList(new ArrayList<Consumer<MenuView>>(b.openHandlers));
        this.closeHandlers = Collections.unmodifiableList(new ArrayList<Consumer<MenuView>>(b.closeHandlers));
        this.refreshTicks = b.refreshTicks;
    }

    /**
     * Starts a menu definition.
     *
     * @return a builder (defaults: 3 rows, empty title)
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the title template.
     *
     * @return title
     */
    public String title() {
        return title;
    }

    /**
     * Returns the number of rows.
     *
     * @return rows, 1-6
     */
    public int rows() {
        return rows;
    }

    /**
     * Returns the slot count.
     *
     * @return {@code rows() * 9}
     */
    public int size() {
        return rows * 9;
    }

    /**
     * Returns whether this menu has pages.
     *
     * @return {@code true} if built with {@link Builder#paginate(Function)}
     */
    public boolean isPaginated() {
        return pageSource != null;
    }

    TextFormat format() {
        return format;
    }

    Map<Integer, Button> buttons() {
        return buttons;
    }

    Button filler() {
        return filler;
    }

    Function<MenuView, List<Button>> pageSource() {
        return pageSource;
    }

    int[] contentSlots() {
        return contentSlots;
    }

    int previousSlot() {
        return previousSlot;
    }

    Button previousButton() {
        return previousButton;
    }

    int nextSlot() {
        return nextSlot;
    }

    Button nextButton() {
        return nextButton;
    }

    List<Consumer<MenuView>> openHandlers() {
        return openHandlers;
    }

    List<Consumer<MenuView>> closeHandlers() {
        return closeHandlers;
    }

    long refreshTicks() {
        return refreshTicks;
    }

    /** Builder for {@link Menu}. Not thread-safe. */
    public static final class Builder {
        private String title = "";
        private int rows = 3;
        private TextFormat format;
        private final Map<Integer, Button> buttons = new HashMap<Integer, Button>();
        private Button filler;
        private String[] layout;
        private final Map<Character, Button> bindings = new HashMap<Character, Button>();
        private Character contentChar;
        private Function<MenuView, List<Button>> pageSource;
        private int[] contentSlots;
        private int previousSlot = -1;
        private Button previousButton;
        private int nextSlot = -1;
        private Button nextButton;
        private final List<Consumer<MenuView>> openHandlers = new ArrayList<Consumer<MenuView>>();
        private final List<Consumer<MenuView>> closeHandlers = new ArrayList<Consumer<MenuView>>();
        private long refreshTicks;

        private Builder() {}

        /**
         * Sets the title.
         *
         * @param template title template; supports {@code {player}}, {@code {page}}, {@code {pages}}
         * @return this builder
         */
        public Builder title(String template) {
            this.title = Objects.requireNonNull(template, "title");
            return this;
        }

        /**
         * Sets the number of rows.
         *
         * @param count 1 to 6
         * @return this builder
         */
        public Builder rows(int count) {
            if (count < 1 || count > 6) {
                throw new IllegalArgumentException("Menus have 1 to 6 rows, got " + count);
            }
            this.rows = count;
            return this;
        }

        /**
         * Sets the text format of the title (defaults to the Bloom default format).
         *
         * @param textFormat format
         * @return this builder
         */
        public Builder format(TextFormat textFormat) {
            this.format = Objects.requireNonNull(textFormat, "format");
            return this;
        }

        /**
         * Places a button, replacing any previous button in the slot.
         *
         * @param slot slot index, {@code 0..size-1}
         * @param button button
         * @return this builder
         */
        public Builder button(int slot, Button button) {
            buttons.put(slot, Objects.requireNonNull(button, "button"));
            return this;
        }

        /**
         * Places a button by row and column (both zero-based).
         *
         * @param row row, {@code 0..rows-1}
         * @param column column, {@code 0..8}
         * @param button button
         * @return this builder
         */
        public Builder button(int row, int column, Button button) {
            if (column < 0 || column > 8) {
                throw new IllegalArgumentException("Column must be 0-8, got " + column);
            }
            return button(row * 9 + column, button);
        }

        /**
         * Places a decorative item.
         *
         * @param slot slot index
         * @param item item
         * @return this builder
         */
        public Builder item(int slot, ItemStack item) {
            return button(slot, Button.of(item));
        }

        /**
         * Places a button in every border slot that is still empty (first/last row and first/last column).
         *
         * @param button border button, usually a filler pane
         * @return this builder
         */
        public Builder border(Button button) {
            for (int slot = 0; slot < rows * 9; slot++) {
                int row = slot / 9;
                int col = slot % 9;
                if ((row == 0 || row == rows - 1 || col == 0 || col == 8) && !buttons.containsKey(slot)) {
                    buttons.put(slot, button);
                }
            }
            return this;
        }

        /**
         * Sets a filler used for every slot that ends up empty after buttons, pagination and hidden buttons are
         * resolved. Content slots of paginated menus are only filled when the current page has no item there.
         *
         * @param button filler button
         * @return this builder
         */
        public Builder fill(Button button) {
            this.filler = Objects.requireNonNull(button, "filler");
            return this;
        }

        /**
         * Describes the menu with one string per row (exactly 9 characters each). Characters are bound to buttons
         * with {@link #bind(char, Button)}; spaces are empty slots. Also sets the row count.
         *
         * @param rowPatterns one pattern per row
         * @return this builder
         */
        public Builder layout(String... rowPatterns) {
            if (rowPatterns.length < 1 || rowPatterns.length > 6) {
                throw new IllegalArgumentException("Layouts have 1 to 6 rows, got " + rowPatterns.length);
            }
            for (int i = 0; i < rowPatterns.length; i++) {
                if (rowPatterns[i].length() != 9) {
                    throw new IllegalArgumentException("Layout row " + i + " must have exactly 9 characters: '"
                            + rowPatterns[i] + "' has " + rowPatterns[i].length());
                }
            }
            this.layout = rowPatterns.clone();
            this.rows = rowPatterns.length;
            return this;
        }

        /**
         * Binds a layout character to a button.
         *
         * @param symbol layout character
         * @param button button
         * @return this builder
         */
        public Builder bind(char symbol, Button button) {
            bindings.put(symbol, Objects.requireNonNull(button, "button"));
            return this;
        }

        /**
         * Applies a reusable template.
         *
         * @param template template
         * @return this builder
         */
        public Builder apply(MenuTemplate template) {
            template.apply(this);
            return this;
        }

        /**
         * Makes the menu paginated. The source is called on every render and returns all content buttons; Bloom
         * slices them into pages.
         *
         * @param source content for a view (the viewer is {@code view.player()})
         * @return this builder
         */
        public Builder paginate(Function<MenuView, List<Button>> source) {
            this.pageSource = Objects.requireNonNull(source, "source");
            return this;
        }

        /**
         * Sets the slots that show paginated content, in order. Default: every slot without a button.
         *
         * @param slots content slots
         * @return this builder
         */
        public Builder contentSlots(int... slots) {
            this.contentSlots = slots.clone();
            return this;
        }

        /**
         * Uses every occurrence of a layout character as a content slot.
         *
         * @param symbol layout character
         * @return this builder
         */
        public Builder contentSlots(char symbol) {
            this.contentChar = symbol;
            return this;
        }

        /**
         * Shows a "previous page" button in a slot when a previous page exists.
         *
         * @param slot slot index
         * @param item button item
         * @return this builder
         */
        public Builder previousPage(int slot, ItemStack item) {
            this.previousSlot = slot;
            this.previousButton = Button.of(item, click -> click.view().previousPage());
            return this;
        }

        /**
         * Shows a "next page" button in a slot when a next page exists.
         *
         * @param slot slot index
         * @param item button item
         * @return this builder
         */
        public Builder nextPage(int slot, ItemStack item) {
            this.nextSlot = slot;
            this.nextButton = Button.of(item, click -> click.view().nextPage());
            return this;
        }

        /**
         * Runs code after the menu was opened for a player (main thread).
         *
         * @param handler handler
         * @return this builder
         */
        public Builder onOpen(Consumer<MenuView> handler) {
            openHandlers.add(Objects.requireNonNull(handler, "handler"));
            return this;
        }

        /**
         * Runs code after the menu was closed for a player, including when navigating to another menu, on
         * disconnect and on plugin shutdown (main thread).
         *
         * @param handler handler
         * @return this builder
         */
        public Builder onClose(Consumer<MenuView> handler) {
            closeHandlers.add(Objects.requireNonNull(handler, "handler"));
            return this;
        }

        /**
         * Re-renders the menu for each viewer every {@code ticks} ticks while it is open (e.g. countdowns).
         *
         * @param ticks period, {@code >= 1}
         * @return this builder
         */
        public Builder refreshEvery(long ticks) {
            if (ticks < 1) {
                throw new IllegalArgumentException("Refresh period must be >= 1 tick, got " + ticks);
            }
            this.refreshTicks = ticks;
            return this;
        }

        /**
         * Builds the menu.
         *
         * @return immutable menu
         */
        public Menu build() {
            int size = rows * 9;
            if (layout != null) {
                for (int row = 0; row < layout.length; row++) {
                    for (int col = 0; col < 9; col++) {
                        char symbol = layout[row].charAt(col);
                        Button bound = bindings.get(symbol);
                        if (bound != null) {
                            buttons.put(row * 9 + col, bound);
                        } else if (symbol != ' ' && (contentChar == null || symbol != contentChar)) {
                            throw new IllegalStateException("Layout character '" + symbol + "' at row " + row
                                    + ", column " + col + " is not bound. Call bind('" + symbol + "', button).");
                        }
                    }
                }
            }
            for (Integer slot : buttons.keySet()) {
                checkSlot(slot, size);
            }
            if (previousButton != null) {
                checkSlot(previousSlot, size);
            }
            if (nextButton != null) {
                checkSlot(nextSlot, size);
            }
            if (pageSource == null && (contentSlots != null || contentChar != null)) {
                throw new IllegalStateException("contentSlots() requires paginate(...).");
            }
            return new Menu(this);
        }

        private int[] resolveContentSlots() {
            if (pageSource == null) {
                return new int[0];
            }
            Set<Integer> reserved = new LinkedHashSet<Integer>(buttons.keySet());
            if (previousButton != null) {
                reserved.add(previousSlot);
            }
            if (nextButton != null) {
                reserved.add(nextSlot);
            }
            List<Integer> slots = new ArrayList<Integer>();
            if (contentSlots != null) {
                for (int slot : contentSlots) {
                    checkSlot(slot, rows * 9);
                    slots.add(slot);
                }
            } else if (contentChar != null && layout != null) {
                for (int row = 0; row < layout.length; row++) {
                    for (int col = 0; col < 9; col++) {
                        if (layout[row].charAt(col) == contentChar) {
                            slots.add(row * 9 + col);
                        }
                    }
                }
            } else {
                for (int slot = 0; slot < rows * 9; slot++) {
                    if (!reserved.contains(slot)) {
                        slots.add(slot);
                    }
                }
            }
            if (slots.isEmpty()) {
                throw new IllegalStateException("A paginated menu needs at least one content slot.");
            }
            int[] result = new int[slots.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = slots.get(i);
            }
            return result;
        }

        private static void checkSlot(int slot, int size) {
            if (slot < 0 || slot >= size) {
                throw new IllegalArgumentException("Slot " + slot + " is outside the menu (0-" + (size - 1) + ").");
            }
        }
    }
}
