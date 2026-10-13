package io.github.crownalonedev.bloom.items;

import org.bukkit.inventory.ItemStack;

/**
 * A custom item defined with {@link Items#define(String, ItemBuilder)}.
 *
 * @since 0.1.0
 */
public final class CustomItem {

    private final Items items;
    private final String id;
    private final ItemStack template;

    CustomItem(Items items, String id, ItemStack template) {
        this.items = items;
        this.id = id;
        this.template = template;
    }

    /**
     * Returns the custom id.
     *
     * @return id
     */
    public String id() {
        return id;
    }

    /**
     * Creates one item.
     *
     * @return a new stack of size 1
     */
    public ItemStack create() {
        return create(1);
    }

    /**
     * Creates a stack.
     *
     * @param amount stack size, {@code >= 1}
     * @return a new stack
     */
    public ItemStack create(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be >= 1, got " + amount);
        }
        ItemStack copy = template.clone();
        copy.setAmount(amount);
        return copy;
    }

    /**
     * Returns whether an item is an instance of this custom item, by persistent id (not by name or lore).
     *
     * @param item item, may be {@code null}
     * @return {@code true} if the item carries this id
     */
    public boolean matches(ItemStack item) {
        return items.isCustom(item, id);
    }

    @Override
    public String toString() {
        return "CustomItem[" + id + "]";
    }
}
