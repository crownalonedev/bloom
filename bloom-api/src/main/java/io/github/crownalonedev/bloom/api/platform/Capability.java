package io.github.crownalonedev.bloom.api.platform;

/**
 * Optional platform features whose availability depends on the server version or implementation.
 *
 * <p>The active adapter decides which capabilities are present, in some cases by probing the running server
 * (for example, legacy NBT access through reflection). Use {@link Capabilities#has(Capability)} before calling
 * features that are not universally available.
 *
 * @since 0.1.0
 */
public enum Capability {
    /** Native Adventure text components (Paper). */
    ADVENTURE_TEXT("native Adventure text components"),
    /** Full MiniMessage parsing. Legacy servers get a documented subset converted to legacy colours instead. */
    MINIMESSAGE("full MiniMessage parsing"),
    /** RGB/hex colours in text (1.16+). */
    HEX_COLORS("RGB hex colours"),
    /** Action bar messages. Legacy 1.8 servers require a successful NMS reflection probe. */
    ACTION_BAR("action bar messages"),
    /** Title and subtitle messages. */
    TITLES("titles and subtitles"),
    /** Custom fade-in/stay/fade-out timings for titles. */
    TITLE_TIMINGS("custom title timings"),
    /** Boss bars. */
    BOSS_BAR("boss bars"),
    /** Component-based inventory titles (allows MiniMessage formatting and titles over 32 characters). */
    COMPONENT_INVENTORY_TITLES("component inventory titles"),
    /** Bukkit PersistentDataContainer on items. */
    PERSISTENT_DATA_CONTAINER("PersistentDataContainer"),
    /** Custom NBT tags on items through version-specific reflection (legacy). */
    ITEM_NBT_TAGS("custom item NBT tags"),
    /** Item flags such as HIDE_ENCHANTS (1.8+). */
    ITEM_FLAGS("item flags"),
    /** Unbreakable items. */
    UNBREAKABLE_ITEMS("unbreakable items"),
    /** Custom model data on items (1.14+). */
    CUSTOM_MODEL_DATA("custom model data"),
    /** Numeric data/durability values selecting item variants, e.g. WOOL:14 (pre-1.13). */
    LEGACY_DATA_VALUES("legacy item data values");

    private final String description;

    Capability(String description) {
        this.description = description;
    }

    /**
     * Returns a short human-readable description.
     *
     * @return description used in error messages and diagnostics
     */
    public String description() {
        return description;
    }
}
