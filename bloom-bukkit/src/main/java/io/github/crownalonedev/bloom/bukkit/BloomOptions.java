package io.github.crownalonedev.bloom.bukkit;

import io.github.crownalonedev.bloom.api.text.TextFormat;
import java.util.Objects;

/**
 * Startup options for {@link Bloom#enable(org.bukkit.plugin.Plugin, BloomOptions)}.
 *
 * <pre>{@code
 * Bloom bloom = Bloom.enable(this, BloomOptions.builder()
 *         .textFormat(TextFormat.MINIMESSAGE)
 *         .unsupportedFeaturePolicy(UnsupportedFeaturePolicy.FALLBACK_TO_CHAT)
 *         .build());
 * }</pre>
 *
 * @since 0.1.0
 */
public final class BloomOptions {

    private static final BloomOptions DEFAULTS = builder().build();

    private final TextFormat textFormat;
    private final UnsupportedFeaturePolicy unsupportedFeaturePolicy;
    private final boolean allowNewerUntestedVersions;
    private final boolean debug;
    private final boolean quietStartup;

    private BloomOptions(Builder builder) {
        this.textFormat = builder.textFormat;
        this.unsupportedFeaturePolicy = builder.unsupportedFeaturePolicy;
        this.allowNewerUntestedVersions = builder.allowNewerUntestedVersions;
        this.debug = builder.debug;
        this.quietStartup = builder.quietStartup;
    }

    /**
     * Returns the default options.
     *
     * @return defaults: {@link TextFormat#LEGACY}, {@link UnsupportedFeaturePolicy#SKIP}, no untested versions,
     *     no debug, full startup banner
     */
    public static BloomOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Creates a builder initialised with the defaults.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the default text format for {@code bloom.messages()} and item/menu text.
     *
     * @return text format
     */
    public TextFormat textFormat() {
        return textFormat;
    }

    /**
     * Returns the default policy for unsupported features.
     *
     * @return policy
     */
    public UnsupportedFeaturePolicy unsupportedFeaturePolicy() {
        return unsupportedFeaturePolicy;
    }

    /**
     * Returns whether Bloom may start on Paper versions newer than the newest supported one.
     *
     * @return opt-in flag
     */
    public boolean allowNewerUntestedVersions() {
        return allowNewerUntestedVersions;
    }

    /**
     * Returns whether debug diagnostics are logged.
     *
     * @return debug flag
     */
    public boolean debug() {
        return debug;
    }

    /**
     * Returns whether the startup banner is reduced to one line.
     *
     * @return quiet flag
     */
    public boolean quietStartup() {
        return quietStartup;
    }

    /** Builder for {@link BloomOptions}. */
    public static final class Builder {
        private TextFormat textFormat = TextFormat.LEGACY;
        private UnsupportedFeaturePolicy unsupportedFeaturePolicy = UnsupportedFeaturePolicy.SKIP;
        private boolean allowNewerUntestedVersions;
        private boolean debug;
        private boolean quietStartup;

        private Builder() {}

        /**
         * Sets the default text format.
         *
         * @param format format used by {@code bloom.messages()} and builders that accept text
         * @return this builder
         */
        public Builder textFormat(TextFormat format) {
            this.textFormat = Objects.requireNonNull(format, "format");
            return this;
        }

        /**
         * Sets the default unsupported-feature policy.
         *
         * @param policy policy
         * @return this builder
         */
        public Builder unsupportedFeaturePolicy(UnsupportedFeaturePolicy policy) {
            this.unsupportedFeaturePolicy = Objects.requireNonNull(policy, "policy");
            return this;
        }

        /**
         * Allows starting on Paper versions newer than this Bloom release supports. Bloom logs a prominent warning.
         * Use only if you test your plugin on that version yourself.
         *
         * @param allow opt-in flag
         * @return this builder
         */
        public Builder allowNewerUntestedVersions(boolean allow) {
            this.allowNewerUntestedVersions = allow;
            return this;
        }

        /**
         * Enables debug diagnostics (adapter probe details, relocation details).
         *
         * @param debug flag
         * @return this builder
         */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * Reduces the startup banner to a single line.
         *
         * @param quiet flag
         * @return this builder
         */
        public Builder quietStartup(boolean quiet) {
            this.quietStartup = quiet;
            return this;
        }

        /**
         * Builds the options.
         *
         * @return immutable options
         */
        public BloomOptions build() {
            return new BloomOptions(this);
        }
    }
}
