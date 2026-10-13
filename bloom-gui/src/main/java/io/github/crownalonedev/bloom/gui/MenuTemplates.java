package io.github.crownalonedev.bloom.gui;

import java.util.Objects;

/**
 * Ready-made {@link MenuTemplate}s.
 *
 * @since 0.1.0
 */
public final class MenuTemplates {

    private MenuTemplates() {}

    /**
     * Fills the border with a button (only empty slots).
     *
     * @param button border button
     * @return template
     */
    public static MenuTemplate border(final Button button) {
        Objects.requireNonNull(button, "button");
        return builder -> builder.border(button);
    }

    /**
     * Fills every slot that ends up empty.
     *
     * @param button filler button
     * @return template
     */
    public static MenuTemplate fill(final Button button) {
        Objects.requireNonNull(button, "button");
        return builder -> builder.fill(button);
    }

    /**
     * Standard paginated layout for {@code rows} rows: content in the inner area, a border, previous/next buttons in
     * the bottom-left and bottom-right corners and an optional close button in the bottom centre.
     *
     * @param rows rows (3-6)
     * @param border border button
     * @param previous previous-page item
     * @param next next-page item
     * @param close close button, or {@code null}
     * @return template; combine with {@link Menu.Builder#paginate}
     */
    public static MenuTemplate paged(
            final int rows,
            final Button border,
            final org.bukkit.inventory.ItemStack previous,
            final org.bukkit.inventory.ItemStack next,
            final Button close) {
        if (rows < 3 || rows > 6) {
            throw new IllegalArgumentException("Paged template needs 3-6 rows, got " + rows);
        }
        return builder -> {
            builder.rows(rows);
            int last = rows * 9;
            builder.previousPage(last - 9, previous);
            builder.nextPage(last - 1, next);
            if (close != null) {
                builder.button(last - 5, close);
            }
            builder.border(border);
        };
    }
}
