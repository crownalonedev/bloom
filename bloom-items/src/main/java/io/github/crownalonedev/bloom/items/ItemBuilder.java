package io.github.crownalonedev.bloom.items;

import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.api.text.TextFormat;
import io.github.crownalonedev.bloom.bukkit.text.FormattedText;
import io.github.crownalonedev.bloom.bukkit.text.Messenger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Fluent, cross-version item builder. Created by {@link Items#builder(String)} or {@link Items#edit(ItemStack)}.
 *
 * <p>Text (name, lore) uses the Bloom default {@link TextFormat} unless {@link #format(TextFormat)} is called, and
 * renders white and non-italic unless the text sets its own colour. Features missing on the running server (e.g.
 * custom model data on 1.8) follow the Bloom {@code UnsupportedFeaturePolicy}: skipped with a one-time warning by
 * default.
 *
 * <p>Builders are not thread-safe; {@link #build()} may be called repeatedly and returns a new item each time.
 *
 * @since 0.1.0
 */
public final class ItemBuilder {

    private final Items items;
    private final ItemStack base;
    private int amount;
    private String name;
    private List<String> lore;
    private final Map<String, Integer> enchantNames = new LinkedHashMap<String, Integer>();
    private final Map<Enchantment, Integer> enchantments = new LinkedHashMap<Enchantment, Integer>();
    private final EnumSet<ItemFlagKey> flags = EnumSet.noneOf(ItemFlagKey.class);
    private Boolean unbreakable;
    private Integer customModelData;
    private boolean glow;
    private final Map<String, String> data = new LinkedHashMap<String, String>();
    private Placeholder[] placeholders = new Placeholder[0];
    private TextFormat format;
    private final List<Consumer<ItemMeta>> metaEditors = new ArrayList<Consumer<ItemMeta>>();

    ItemBuilder(Items items, ItemStack base) {
        this.items = items;
        this.base = base;
        this.amount = base.getAmount();
    }

    /**
     * Sets the stack size.
     *
     * @param newAmount amount, {@code 1..64} (or the item's max stack size)
     * @return this builder
     */
    public ItemBuilder amount(int newAmount) {
        if (newAmount < 1 || newAmount > 64) {
            throw new IllegalArgumentException("amount must be between 1 and 64, got " + newAmount);
        }
        this.amount = newAmount;
        return this;
    }

    /**
     * Sets the display name.
     *
     * @param template name in the builder's text format, e.g. {@code "&dBlossom Blade"}
     * @return this builder
     */
    public ItemBuilder name(String template) {
        this.name = Objects.requireNonNull(template, "name");
        return this;
    }

    /**
     * Replaces the lore.
     *
     * @param lines lore lines in the builder's text format
     * @return this builder
     */
    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    /**
     * Replaces the lore.
     *
     * @param lines lore lines in the builder's text format
     * @return this builder
     */
    public ItemBuilder lore(List<String> lines) {
        this.lore = new ArrayList<String>(lines);
        return this;
    }

    /**
     * Appends lore lines.
     *
     * @param lines lines to append
     * @return this builder
     */
    public ItemBuilder addLore(String... lines) {
        if (lore == null) {
            lore = new ArrayList<String>();
        }
        Collections.addAll(lore, lines);
        return this;
    }

    /**
     * Adds an enchantment by name. Accepts modern keys ({@code "sharpness"}) and legacy Bukkit names
     * ({@code "DAMAGE_ALL"}). Enchantments unknown on this server are skipped with a warning. Levels above the
     * vanilla maximum are allowed.
     *
     * @param enchantment enchantment key or name
     * @param level level, {@code >= 1}
     * @return this builder
     */
    public ItemBuilder enchant(String enchantment, int level) {
        enchantNames.put(Objects.requireNonNull(enchantment, "enchantment"), checkLevel(level));
        return this;
    }

    /**
     * Adds an enchantment object from the API you compile against.
     *
     * @param enchantment enchantment
     * @param level level, {@code >= 1}
     * @return this builder
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        enchantments.put(Objects.requireNonNull(enchantment, "enchantment"), checkLevel(level));
        return this;
    }

    /**
     * Adds item flags. Requires {@code ITEM_FLAGS} (1.8+).
     *
     * @param newFlags flags
     * @return this builder
     */
    public ItemBuilder flags(ItemFlagKey... newFlags) {
        Collections.addAll(flags, newFlags);
        return this;
    }

    /**
     * Makes the item glow without visible enchantments (see {@code ItemPlatform#glow}).
     *
     * @return this builder
     */
    public ItemBuilder glow() {
        this.glow = true;
        return this;
    }

    /**
     * Sets the unbreakable flag. Requires {@code UNBREAKABLE_ITEMS}.
     *
     * @param value unbreakable
     * @return this builder
     */
    public ItemBuilder unbreakable(boolean value) {
        this.unbreakable = value;
        return this;
    }

    /**
     * Sets custom model data. Requires {@code CUSTOM_MODEL_DATA} (modern only).
     *
     * @param value model data
     * @return this builder
     */
    public ItemBuilder customModelData(int value) {
        this.customModelData = value;
        return this;
    }

    /**
     * Stores a string in the item's persistent data under the plugin namespace.
     *
     * @param key key, {@code [a-z0-9._-]+}
     * @param value value
     * @return this builder
     */
    public ItemBuilder data(String key, String value) {
        data.put(Items.checkKey(key), Objects.requireNonNull(value, "value"));
        return this;
    }

    /**
     * Marks the item with a custom id (see {@link Items#customId(ItemStack)}).
     *
     * @param id custom id
     * @return this builder
     */
    public ItemBuilder customId(String id) {
        return data(Items.ID_KEY, id);
    }

    /**
     * Sets placeholders applied to the name and lore.
     *
     * @param values placeholders
     * @return this builder
     */
    public ItemBuilder placeholders(Placeholder... values) {
        this.placeholders = values.clone();
        return this;
    }

    /**
     * Sets the text format for name and lore.
     *
     * @param textFormat format
     * @return this builder
     */
    public ItemBuilder format(TextFormat textFormat) {
        this.format = Objects.requireNonNull(textFormat, "format");
        return this;
    }

    /**
     * Applies a custom edit to the Bukkit {@link ItemMeta} after Bloom's own settings. Use for features Bloom does
     * not wrap (leather colour, skull owner, ...). Code here must be valid for every server version you target.
     *
     * @param editor meta editor
     * @return this builder
     */
    public ItemBuilder meta(Consumer<ItemMeta> editor) {
        metaEditors.add(Objects.requireNonNull(editor, "editor"));
        return this;
    }

    /**
     * Builds a new item.
     *
     * @return the item
     */
    public ItemStack build() {
        ItemStack item = base.clone();
        item.setAmount(amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            applyMeta(meta);
            item.setItemMeta(meta);
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            item = items.setData(item, entry.getKey(), entry.getValue());
        }
        return item;
    }

    private void applyMeta(ItemMeta meta) {
        Messenger messenger = items.bloom().messages();
        if (format != null) {
            messenger = messenger.withFormat(format);
        }
        if (name != null) {
            items.platform().displayName(meta, messenger.render(name, placeholders));
        }
        if (lore != null) {
            List<FormattedText> rendered = new ArrayList<FormattedText>(lore.size());
            for (String line : lore) {
                rendered.add(messenger.render(line, placeholders));
            }
            items.platform().lore(meta, rendered);
        }
        for (Map.Entry<String, Integer> entry : enchantNames.entrySet()) {
            Optional<Enchantment> enchantment = items.platform().enchantment(entry.getKey());
            if (enchantment.isPresent()) {
                meta.addEnchant(enchantment.get(), entry.getValue(), true);
            } else {
                items.warn("Unknown enchantment '" + entry.getKey() + "' on "
                        + items.bloom().platform().describe() + "; it was skipped.");
            }
        }
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        if (!flags.isEmpty() && items.feature(Capability.ITEM_FLAGS, "ItemBuilder.flags")) {
            List<String> unknown = items.platform().addItemFlags(meta, Items.flagNames(flags));
            for (ItemFlagKey flag : flags) {
                if (unknown.containsAll(flag.bukkitNames())) {
                    items.warn("Item flag " + flag + " does not exist on "
                            + items.bloom().platform().describe() + "; it was skipped.");
                }
            }
        }
        if (unbreakable != null && items.feature(Capability.UNBREAKABLE_ITEMS, "ItemBuilder.unbreakable")) {
            items.platform().unbreakable(meta, unbreakable);
        }
        if (customModelData != null && items.feature(Capability.CUSTOM_MODEL_DATA, "ItemBuilder.customModelData")) {
            items.platform().customModelData(meta, customModelData);
        }
        if (glow) {
            items.platform().glow(meta);
        }
        for (Consumer<ItemMeta> editor : metaEditors) {
            editor.accept(meta);
        }
    }

    private static int checkLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Enchantment level must be >= 1, got " + level);
        }
        return level;
    }
}
