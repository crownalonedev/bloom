package io.github.crownalonedev.bloom.bukkit.text;

import java.time.Duration;
import java.util.Objects;

/**
 * A title/subtitle pair with display timings.
 *
 * <p>Timings require {@code Capability.TITLE_TIMINGS}; on 1.8 servers the client's default timings are used
 * (fade in 0.5s, stay 3.5s, fade out 1s) and custom timings are ignored.
 *
 * <pre>{@code
 * messenger.title(player, Title.of("&dWelcome", "&7to the server"));
 * messenger.title(player, Title.of("<gold>Victory").times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(500)));
 * }</pre>
 *
 * @since 0.1.0
 */
public final class Title {

    /** Default fade-in time, matching the vanilla client. */
    public static final Duration DEFAULT_FADE_IN = Duration.ofMillis(500);
    /** Default stay time, matching the vanilla client. */
    public static final Duration DEFAULT_STAY = Duration.ofMillis(3500);
    /** Default fade-out time, matching the vanilla client. */
    public static final Duration DEFAULT_FADE_OUT = Duration.ofMillis(1000);

    private final String title;
    private final String subtitle;
    private final Duration fadeIn;
    private final Duration stay;
    private final Duration fadeOut;

    private Title(String title, String subtitle, Duration fadeIn, Duration stay, Duration fadeOut) {
        this.title = Objects.requireNonNull(title, "title");
        this.subtitle = Objects.requireNonNull(subtitle, "subtitle");
        this.fadeIn = requireNonNegative(fadeIn, "fadeIn");
        this.stay = requireNonNegative(stay, "stay");
        this.fadeOut = requireNonNegative(fadeOut, "fadeOut");
    }

    /**
     * Creates a title without a subtitle and with default timings.
     *
     * @param title title text in the messenger's format
     * @return the title
     */
    public static Title of(String title) {
        return of(title, "");
    }

    /**
     * Creates a title with a subtitle and default timings.
     *
     * @param title title text in the messenger's format
     * @param subtitle subtitle text in the messenger's format
     * @return the title
     */
    public static Title of(String title, String subtitle) {
        return new Title(title, subtitle, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT);
    }

    /**
     * Returns a copy with custom timings.
     *
     * @param fadeIn fade-in duration
     * @param stay stay duration
     * @param fadeOut fade-out duration
     * @return a new title
     */
    public Title times(Duration fadeIn, Duration stay, Duration fadeOut) {
        return new Title(title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Returns the title template.
     *
     * @return title text
     */
    public String title() {
        return title;
    }

    /**
     * Returns the subtitle template.
     *
     * @return subtitle text, possibly empty
     */
    public String subtitle() {
        return subtitle;
    }

    /**
     * Returns the fade-in duration.
     *
     * @return fade-in
     */
    public Duration fadeIn() {
        return fadeIn;
    }

    /**
     * Returns the stay duration.
     *
     * @return stay
     */
    public Duration stay() {
        return stay;
    }

    /**
     * Returns the fade-out duration.
     *
     * @return fade-out
     */
    public Duration fadeOut() {
        return fadeOut;
    }

    private static Duration requireNonNegative(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
        return value;
    }
}
