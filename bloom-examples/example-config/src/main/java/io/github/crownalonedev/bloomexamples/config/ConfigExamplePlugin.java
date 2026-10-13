package io.github.crownalonedev.bloomexamples.config;

import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.api.util.Durations;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.config.ConfigException;
import io.github.crownalonedev.bloom.config.YamlConfig;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bloom Config example: bundled defaults, POJO mapping, validation, a migration and safe reloads.
 *
 * <p>Version 1 of this config had a top-level {@code prefix}; version 2 moved it to {@code messages.prefix}. Bloom
 * migrates old files automatically and keeps a backup.
 */
public final class ConfigExamplePlugin extends JavaPlugin {

    /** Mapped from the {@code homes} section. Field names map to kebab-case keys. */
    static final class HomeSettings {
        int maxHomes = 3;
        Duration teleportDelay = Duration.ofSeconds(3);
        List<String> disabledWorlds = new ArrayList<String>();
    }

    private Bloom bloom;
    private YamlConfig config;
    private HomeSettings homes;

    @Override
    public void onEnable() {
        bloom = Bloom.enable(this);
        config = YamlConfig.builder(getDataFolder().toPath().resolve("config.yml"))
                .defaults(() -> getResource("config.yml"))
                .version(2)
                .migration(1, migration -> migration.move("prefix", "messages.prefix"))
                .logger(getLogger())
                .build();
        applyConfig();
        getCommand("bloomconfig").setExecutor(this);
    }

    private void applyConfig() {
        config.validate()
                .require("messages.prefix")
                .range("homes.max-homes", 1, 100)
                .type("homes.teleport-delay", Duration.class)
                .check();
        homes = config.get("homes", HomeSettings.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = config.getString("messages.prefix");
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            try {
                config.reload();
                applyConfig();
                bloom.messages().send(sender, prefix + "&aConfiguration reloaded.");
            } catch (ConfigException e) {
                // The previous values stay active when the new file is invalid.
                bloom.messages()
                        .send(sender, prefix + "&cReload failed: {error}", Placeholder.of("error", e.getMessage()));
            }
            return true;
        }
        bloom.messages()
                .send(
                        sender,
                        prefix + "&7Max homes: &f{max}&7, teleport delay: &f{delay}&7, disabled in: &f{worlds}",
                        Placeholder.of("max", homes.maxHomes),
                        Placeholder.of("delay", Durations.format(homes.teleportDelay)),
                        Placeholder.of("worlds", homes.disabledWorlds));
        return true;
    }

    @Override
    public void onDisable() {
        if (bloom != null) {
            bloom.shutdown();
        }
    }
}
