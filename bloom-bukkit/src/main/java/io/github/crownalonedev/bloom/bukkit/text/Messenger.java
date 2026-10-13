package io.github.crownalonedev.bloom.bukkit.text;

import io.github.crownalonedev.bloom.api.platform.Capabilities;
import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.api.text.TextFormat;
import io.github.crownalonedev.bloom.bukkit.UnsupportedFeaturePolicy;
import io.github.crownalonedev.bloom.bukkit.internal.BossBars;
import io.github.crownalonedev.bloom.bukkit.internal.NoopBossBar;
import io.github.crownalonedev.bloom.bukkit.platform.PlayerPlatform;
import io.github.crownalonedev.bloom.bukkit.platform.TextPlatform;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Sends formatted text to players and the console on every supported platform.
 *
 * <p>Obtain the default instance with {@code bloom.messages()}; derive variants with {@link #withPrefix(String)},
 * {@link #withFormat(TextFormat)} and {@link #withUnsupportedPolicy(UnsupportedFeaturePolicy)}. Messengers are
 * immutable and thread-safe.
 *
 * <pre>{@code
 * Messenger messages = bloom.messages().withPrefix("&d&lBloom &8» ");
 * messages.send(player, "&7Welcome back, &f{player}&7!", Placeholder.of("player", player.getName()));
 * messages.actionBar(player, "&aSaved!");
 * messages.title(player, Title.of("&dLevel up", "&7You reached level {level}"), Placeholder.of("level", 5));
 * }</pre>
 *
 * <h2>Threads</h2>
 * Chat, action bars and titles may be sent from any thread. Boss bars must be created and changed on the main
 * thread.
 *
 * <h2>Unsupported features</h2>
 * Action bars, titles and boss bars depend on {@link Capability capabilities}. When one is missing, behaviour
 * follows the {@link UnsupportedFeaturePolicy}: skip with a one-time warning (default), fall back to chat, or throw.
 *
 * @since 0.1.0
 */
public final class Messenger {

    private final Shared shared;
    private final TextFormat format;
    private final String prefix;
    private final UnsupportedFeaturePolicy policy;

    /** State shared by a messenger and all variants derived from it. */
    private static final class Shared {
        final TextPlatform text;
        final PlayerPlatform players;
        final Capabilities capabilities;
        final Logger logger;
        final Consumer<AutoCloseable> closeOnShutdown;
        final Set<Capability> warned = Collections.synchronizedSet(EnumSet.noneOf(Capability.class));

        Shared(
                TextPlatform text,
                PlayerPlatform players,
                Capabilities capabilities,
                Logger logger,
                Consumer<AutoCloseable> closeOnShutdown) {
            this.text = text;
            this.players = players;
            this.capabilities = capabilities;
            this.logger = logger;
            this.closeOnShutdown = closeOnShutdown;
        }
    }

    /**
     * Creates a messenger. <strong>Internal:</strong> plugin code obtains messengers from {@code bloom.messages()}.
     *
     * @param text adapter text platform
     * @param players adapter player platform
     * @param capabilities platform capabilities
     * @param logger plugin logger
     * @param closeOnShutdown registers boss bars so they are closed at shutdown
     * @param format default format
     * @param policy default unsupported-feature policy
     */
    public Messenger(
            TextPlatform text,
            PlayerPlatform players,
            Capabilities capabilities,
            Logger logger,
            Consumer<AutoCloseable> closeOnShutdown,
            TextFormat format,
            UnsupportedFeaturePolicy policy) {
        this(new Shared(text, players, capabilities, logger, closeOnShutdown), format, "", policy);
    }

    private Messenger(Shared shared, TextFormat format, String prefix, UnsupportedFeaturePolicy policy) {
        this.shared = shared;
        this.format = Objects.requireNonNull(format, "format");
        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    /**
     * Returns the text format this messenger parses templates with.
     *
     * @return format
     */
    public TextFormat format() {
        return format;
    }

    /**
     * Returns the chat prefix template.
     *
     * @return prefix, empty if none
     */
    public String prefix() {
        return prefix;
    }

    /**
     * Returns the unsupported-feature policy.
     *
     * @return policy
     */
    public UnsupportedFeaturePolicy unsupportedPolicy() {
        return policy;
    }

    /**
     * Returns a messenger that parses templates in another format.
     *
     * @param newFormat format
     * @return derived messenger
     */
    public Messenger withFormat(TextFormat newFormat) {
        return new Messenger(shared, newFormat, prefix, policy);
    }

    /**
     * Returns a messenger that prepends a prefix to chat messages (not to action bars, titles or boss bars). The
     * prefix is written in this messenger's format.
     *
     * @param newPrefix prefix template, e.g. {@code "&d[Shop] "}
     * @return derived messenger
     */
    public Messenger withPrefix(String newPrefix) {
        return new Messenger(shared, format, newPrefix, policy);
    }

    /**
     * Returns a messenger with another unsupported-feature policy.
     *
     * @param newPolicy policy
     * @return derived messenger
     */
    public Messenger withUnsupportedPolicy(UnsupportedFeaturePolicy newPolicy) {
        return new Messenger(shared, format, prefix, newPolicy);
    }

    /**
     * Applies placeholders to a template using this messenger's format.
     *
     * @param template template text
     * @param placeholders placeholders
     * @return prepared text for adapter APIs
     */
    public FormattedText render(String template, Placeholder... placeholders) {
        return FormattedText.prepare(template, format, shared.capabilities.has(Capability.HEX_COLORS), placeholders);
    }

    /**
     * Renders a template to a legacy {@code §}-formatted string, for APIs that only accept strings (scoreboards,
     * signs on old versions).
     *
     * @param template template text
     * @param placeholders placeholders
     * @return legacy string
     */
    public String legacy(String template, Placeholder... placeholders) {
        return shared.text.toLegacyString(render(template, placeholders));
    }

    /**
     * Sends a chat message with this messenger's prefix.
     *
     * @param recipient player or console
     * @param message message template
     * @param placeholders placeholder values
     */
    public void send(CommandSender recipient, String message, Placeholder... placeholders) {
        shared.text.sendMessage(recipient, render(prefix + message, placeholders));
    }

    /**
     * Sends a chat message to several recipients. The template is rendered once.
     *
     * @param recipients players or console
     * @param message message template
     * @param placeholders placeholder values
     */
    public void send(Iterable<? extends CommandSender> recipients, String message, Placeholder... placeholders) {
        FormattedText rendered = render(prefix + message, placeholders);
        for (CommandSender recipient : recipients) {
            shared.text.sendMessage(recipient, rendered);
        }
    }

    /**
     * Sends a chat message to every online player and the console.
     *
     * @param message message template
     * @param placeholders placeholder values
     */
    public void broadcast(String message, Placeholder... placeholders) {
        FormattedText rendered = render(prefix + message, placeholders);
        for (Player player : shared.players.onlinePlayers()) {
            shared.text.sendMessage(player, rendered);
        }
        shared.text.sendMessage(Bukkit.getConsoleSender(), rendered);
    }

    /**
     * Shows an action bar message.
     *
     * @param player recipient
     * @param message message template (no prefix is added)
     * @param placeholders placeholder values
     * @return {@code true} if delivered as an action bar or as a chat fallback
     * @throws io.github.crownalonedev.bloom.api.UnsupportedCapabilityException if unsupported and the policy is
     *     {@code THROW}
     */
    public boolean actionBar(Player player, String message, Placeholder... placeholders) {
        FormattedText rendered = render(message, placeholders);
        if (shared.capabilities.has(Capability.ACTION_BAR)) {
            shared.text.sendActionBar(player, rendered);
            return true;
        }
        return unsupported(Capability.ACTION_BAR, "Messenger.actionBar", player, rendered);
    }

    /**
     * Shows a title.
     *
     * @param player recipient
     * @param title title and subtitle templates with timings
     * @param placeholders placeholder values, applied to both lines
     * @return {@code true} if delivered as a title or as a chat fallback
     * @throws io.github.crownalonedev.bloom.api.UnsupportedCapabilityException if unsupported and the policy is
     *     {@code THROW}
     */
    public boolean title(Player player, Title title, Placeholder... placeholders) {
        FormattedText main = render(title.title(), placeholders);
        FormattedText sub = render(title.subtitle(), placeholders);
        if (shared.capabilities.has(Capability.TITLES)) {
            shared.text.sendTitle(player, main, sub, title.fadeIn(), title.stay(), title.fadeOut());
            return true;
        }
        FormattedText chat = sub.isEmpty()
                ? main
                : FormattedText.ofPrepared(
                        main.content() + (format == TextFormat.LEGACY ? " §r" : " <reset>") + sub.content(), format);
        return unsupported(Capability.TITLES, "Messenger.title", player, chat);
    }

    /**
     * Creates a boss bar. Show it to players with {@link BloomBossBar#show(Player)}. Must be called on the main
     * thread. Bloom closes the bar on shutdown.
     *
     * @param title title template
     * @param progress progress between {@code 0} and {@code 1}
     * @param color colour
     * @param style style
     * @param placeholders placeholder values for the initial title
     * @return a boss bar, or a non-functional bar ({@link BloomBossBar#isFunctional()} is {@code false}) when boss
     *     bars are unsupported and the policy is not {@code THROW}
     * @throws io.github.crownalonedev.bloom.api.UnsupportedCapabilityException if unsupported and the policy is
     *     {@code THROW}
     */
    public BloomBossBar bossBar(
            String title, float progress, BossBarColor color, BossBarStyle style, Placeholder... placeholders) {
        BossBars.checkProgress(progress);
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(style, "style");
        if (!shared.capabilities.has(Capability.BOSS_BAR)) {
            unsupported(Capability.BOSS_BAR, "Messenger.bossBar", null, null);
            return new NoopBossBar();
        }
        TextPlatform.TitleRenderer renderer = new TextPlatform.TitleRenderer() {
            @Override
            public FormattedText render(String template) {
                return Messenger.this.render(template);
            }
        };
        BloomBossBar bar = shared.text.createBossBar(render(title, placeholders), progress, color, style, renderer);
        shared.closeOnShutdown.accept(bar);
        return bar;
    }

    private boolean unsupported(Capability capability, String feature, Player player, FormattedText fallback) {
        if (policy == UnsupportedFeaturePolicy.THROW) {
            shared.capabilities.require(capability, feature);
        }
        if (policy == UnsupportedFeaturePolicy.FALLBACK_TO_CHAT && player != null && fallback != null) {
            shared.text.sendMessage(player, fallback);
            return true;
        }
        if (shared.warned.add(capability)) {
            shared.logger.warning(feature + " was skipped: " + capability.description() + " are not available on "
                    + shared.capabilities.platformDescription() + ". Check bloom.capabilities().has(Capability."
                    + capability.name() + ") or choose UnsupportedFeaturePolicy.FALLBACK_TO_CHAT. "
                    + "(This warning is shown once.)");
        }
        return false;
    }
}
