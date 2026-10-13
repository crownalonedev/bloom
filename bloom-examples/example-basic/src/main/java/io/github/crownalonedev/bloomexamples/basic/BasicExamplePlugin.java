package io.github.crownalonedev.bloomexamples.basic;

import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.api.util.Cooldowns;
import io.github.crownalonedev.bloom.api.util.Durations;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.bukkit.text.Messenger;
import io.github.crownalonedev.bloom.bukkit.text.Title;
import java.time.Duration;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The smallest useful Bloom plugin: lifecycle, messages with placeholders, an event subscription, a repeating task
 * and a cooldown. Runs unchanged on 1.7.10 through 26.x.
 */
public final class BasicExamplePlugin extends JavaPlugin {

    private final Cooldowns<UUID> helloCooldown = Cooldowns.of(Duration.ofSeconds(5));
    private Bloom bloom;
    private Messenger messages;

    @Override
    public void onEnable() {
        bloom = Bloom.enable(this);
        messages = bloom.messages().withPrefix("&d&lBloom &8» &7");

        bloom.events()
                .subscribe(
                        PlayerJoinEvent.class,
                        event -> messages.send(
                                event.getPlayer(),
                                "Welcome, &f{player}&7! This server runs {platform}.",
                                Placeholder.of("player", event.getPlayer().getName()),
                                Placeholder.of("platform", bloom.platform().describe())));

        bloom.scheduler()
                .syncRepeating(
                        task -> messages.broadcast("Tip: type &f/hello &7to try Bloom messages."),
                        Duration.ofMinutes(5),
                        Duration.ofMinutes(5));

        getCommand("hello").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messages.send(
                    sender,
                    "Hello, console! Bloom {version} is running.",
                    Placeholder.of("version", io.github.crownalonedev.bloom.api.BloomVersion.VERSION));
            return true;
        }
        Player player = (Player) sender;
        if (!helloCooldown.tryUse(player.getUniqueId())) {
            messages.send(
                    player,
                    "&cSlow down! Try again in {time}.",
                    Placeholder.of("time", Durations.format(helloCooldown.remaining(player.getUniqueId()))));
            return true;
        }
        messages.send(player, "Hello, &f{player}&7!", Placeholder.of("player", player.getName()));
        // Action bars and titles depend on the server version. With the default policy they are skipped (with a
        // one-time console warning) on servers that lack them, e.g. 1.7.10.
        messages.actionBar(player, "&dYou said hello!");
        messages.title(player, Title.of("&dHello", "&7from Bloom"));
        return true;
    }

    @Override
    public void onDisable() {
        if (bloom != null) {
            bloom.shutdown();
        }
    }
}
