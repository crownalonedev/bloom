package io.github.crownalonedev.bloom.gui;

/**
 * A reusable piece of menu layout, applied with {@link Menu.Builder#apply(MenuTemplate)}.
 *
 * <pre>{@code
 * MenuTemplate branded = builder -> builder
 *         .border(Button.of(pinkPane))
 *         .button(4, Button.of(logo));
 *
 * Menu menu = Menu.builder().rows(5).apply(branded).title("&dProfile").build();
 * }</pre>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface MenuTemplate {

    /**
     * Applies this template to a builder.
     *
     * @param builder menu builder
     */
    void apply(Menu.Builder builder);
}
