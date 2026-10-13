package io.github.crownalonedev.bloom.bukkit.internal;

/** Shared boss bar validation. */
public final class BossBars {

    private BossBars() {}

    /**
     * Validates a progress value.
     *
     * @param progress value to check
     * @return the value
     * @throws IllegalArgumentException if not within {@code 0..1}
     */
    public static float checkProgress(float progress) {
        if (!(progress >= 0f && progress <= 1f)) {
            throw new IllegalArgumentException("Boss bar progress must be between 0.0 and 1.0, got " + progress);
        }
        return progress;
    }
}
