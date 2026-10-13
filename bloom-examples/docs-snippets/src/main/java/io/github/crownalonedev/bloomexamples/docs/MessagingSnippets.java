package io.github.crownalonedev.bloomexamples.docs;

import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.api.text.TextFormat;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.bukkit.UnsupportedFeaturePolicy;
import io.github.crownalonedev.bloom.bukkit.text.BloomBossBar;
import io.github.crownalonedev.bloom.bukkit.text.BossBarColor;
import io.github.crownalonedev.bloom.bukkit.text.BossBarStyle;
import io.github.crownalonedev.bloom.bukkit.text.Messenger;
import io.github.crownalonedev.bloom.bukkit.text.Title;
import java.time.Duration;
import org.bukkit.entity.Player;

/** Messaging snippets used in the documentation. */
final class MessagingSnippets {

    private MessagingSnippets() {}

    static void send(Bloom bloom, Player player) {
        // @snippet:messaging.send
        Messenger messages = bloom.messages().withPrefix("&d&lShop &8» &7");
        messages.send(
                player,
                "You bought &f{amount}x {item}&7.",
                Placeholder.of("amount", 3),
                Placeholder.of("item", "Golden Apple"));
        messages.broadcast("&f{player} &7joined the arena!", Placeholder.of("player", player.getName()));
        // @end
    }

    static void escaping(Bloom bloom, Player player, String chatMessage) {
        // @snippet:messaging.escaping
        // Placeholder.of: plain text. "&c" or "<red>" inside chatMessage is shown literally.
        bloom.messages().send(player, "&7[Chat] &f{message}", Placeholder.of("message", chatMessage));
        // Placeholder.formatted: trusted text that should keep its formatting.
        bloom.messages().send(player, "Rank: {rank}", Placeholder.formatted("rank", "&6&lVIP"));
        // @end
    }

    static void miniMessage(Bloom bloom, Player player) {
        // @snippet:messaging.minimessage
        Messenger mini = bloom.messages().withFormat(TextFormat.MINIMESSAGE);
        mini.send(
                player,
                "<gradient:#F2A7CC:#B9A0DD>Welcome</gradient> <gray>to the server, <white>{player}</white>!",
                Placeholder.of("player", player.getName()));
        // On 1.7/1.8 the gradient uses its first colour; colours, bold, italic and so on convert exactly.
        // @end
    }

    static void titles(Bloom bloom, Player player) {
        // @snippet:messaging.titles
        Messenger messages = bloom.messages();
        messages.actionBar(player, "&a+5 coins");
        messages.title(
                player,
                Title.of("&dRound 2", "&7Fight!")
                        .times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(500)));
        // Returns false when skipped (e.g. action bars on 1.7.10 with the default SKIP policy).
        boolean shown = messages.actionBar(player, "&eCapturing...");
        // @end
        if (!shown) {
            messages.send(player, "&eCapturing...");
        }
    }

    static void policy(Bloom bloom, Player player) {
        // @snippet:messaging.policy
        Messenger strict = bloom.messages().withUnsupportedPolicy(UnsupportedFeaturePolicy.THROW);
        Messenger lenient = bloom.messages().withUnsupportedPolicy(UnsupportedFeaturePolicy.FALLBACK_TO_CHAT);
        lenient.title(player, Title.of("&6Victory")); // a chat line on servers without titles
        // @end
        strict.hashCode();
    }

    static void bossBar(Bloom bloom, Player player) {
        // @snippet:messaging.bossbar
        BloomBossBar bar = bloom.messages()
                .bossBar(
                        "&dEvent ends in {time}",
                        1.0f,
                        BossBarColor.PINK,
                        BossBarStyle.SEGMENTED_10,
                        Placeholder.of("time", "5:00"));
        bar.show(player);
        bar.progress(0.5f).title("&dEvent ends in 2:30");
        bar.close(); // also closed automatically on shutdown
        // isFunctional() is false on servers without boss bars (unless the policy is THROW, which throws instead)
        // @end
    }
}
