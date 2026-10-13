package io.github.crownalonedev.bloom.gui;

/**
 * Version-independent click types. Mapped from Bukkit's {@code ClickType} by name, so constants added in newer
 * versions never cause linkage errors on older servers.
 *
 * @since 0.1.0
 */
public enum ClickKind {
    /** Left click. */
    LEFT,
    /** Right click. */
    RIGHT,
    /** Shift + left click. */
    SHIFT_LEFT,
    /** Shift + right click. */
    SHIFT_RIGHT,
    /** Middle click (creative pick). */
    MIDDLE,
    /** Number key 1-9; see {@link ClickContext#hotbarButton()}. */
    NUMBER_KEY,
    /** Drop key (Q). */
    DROP,
    /** Control + drop key. */
    CONTROL_DROP,
    /** Off-hand swap key (F), 1.16+. */
    SWAP_OFFHAND,
    /** Any other click (creative, window border, unknown). */
    OTHER;

    /**
     * Maps a Bukkit {@code ClickType} constant name.
     *
     * @param bukkitName {@code ClickType#name()}
     * @return the kind; {@code null} for {@code DOUBLE_CLICK}, which is the second event of a double click and is
     *     never dispatched to buttons
     */
    static ClickKind fromBukkit(String bukkitName) {
        if ("DOUBLE_CLICK".equals(bukkitName)) {
            return null;
        }
        for (ClickKind kind : values()) {
            if (kind.name().equals(bukkitName)) {
                return kind;
            }
        }
        return OTHER;
    }

    /**
     * Returns whether this is a left click (with or without shift).
     *
     * @return {@code true} for {@link #LEFT} and {@link #SHIFT_LEFT}
     */
    public boolean isLeft() {
        return this == LEFT || this == SHIFT_LEFT;
    }

    /**
     * Returns whether this is a right click (with or without shift).
     *
     * @return {@code true} for {@link #RIGHT} and {@link #SHIFT_RIGHT}
     */
    public boolean isRight() {
        return this == RIGHT || this == SHIFT_RIGHT;
    }

    /**
     * Returns whether shift was held.
     *
     * @return {@code true} for shift clicks
     */
    public boolean isShift() {
        return this == SHIFT_LEFT || this == SHIFT_RIGHT;
    }
}
