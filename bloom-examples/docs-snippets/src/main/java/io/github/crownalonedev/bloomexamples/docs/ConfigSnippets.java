package io.github.crownalonedev.bloomexamples.docs;

import io.github.crownalonedev.bloom.config.ConfigException;
import io.github.crownalonedev.bloom.config.ConfigKey;
import io.github.crownalonedev.bloom.config.ConfigSection;
import io.github.crownalonedev.bloom.config.TypeSerializer;
import io.github.crownalonedev.bloom.config.YamlConfig;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/** Configuration snippets used in the documentation. */
final class ConfigSnippets {

    private ConfigSnippets() {}

    static YamlConfig load(JavaPlugin plugin) {
        // @snippet:config.load
        YamlConfig config = YamlConfig.builder(plugin.getDataFolder().toPath().resolve("config.yml"))
                .defaults(() -> plugin.getResource("config.yml")) // bundled in src/main/resources
                .logger(plugin.getLogger())
                .build(); // creates the file from defaults if missing, then loads it
        // @end
        return config;
    }

    static void getters(YamlConfig config) {
        // @snippet:config.getters
        String prefix = config.getString("messages.prefix"); // required: throws if missing everywhere
        int maxHomes = config.getInt("homes.max", 3); // with a fallback
        Duration delay = config.getDuration("homes.teleport-delay"); // "3s", "1m30s" or a number of seconds
        List<String> worlds = config.getStringList("homes.disabled-worlds");
        ConfigSection kits = config.section("kits");
        for (String kit : kits.keys()) {
            int cost = kits.getInt(kit + ".cost", 0);
            // ...
            Integer.valueOf(cost).hashCode();
        }
        // @end
        prefix.hashCode();
        Integer.valueOf(maxHomes).hashCode();
        delay.hashCode();
        worlds.hashCode();
    }

    // @snippet:config.pojo
    static final class HomeSettings {
        int maxHomes = 3; // key "max-homes"; the value here is the default when the key is missing
        Duration teleportDelay = Duration.ofSeconds(3);
        List<String> disabledWorlds = new ArrayList<String>();

        @ConfigKey("allow-in-combat")
        boolean combat = false;
    }

    static HomeSettings homes(YamlConfig config) {
        return config.get("homes", HomeSettings.class);
    }
    // @end

    static void validate(YamlConfig config) {
        // @snippet:config.validate
        config.validate()
                .require("storage.type")
                .oneOf("storage.type", "sqlite", "mysql", "mariadb")
                .range("homes.max-homes", 1, 100)
                .type("homes.teleport-delay", Duration.class)
                .check(); // throws ConfigException listing every problem with its line number
        // @end
    }

    static YamlConfig migrate(JavaPlugin plugin) {
        // @snippet:config.migrate
        return YamlConfig.builder(plugin.getDataFolder().toPath().resolve("config.yml"))
                .defaults(() -> plugin.getResource("config.yml"))
                .version(3) // files without "config-version" count as version 1
                .migration(1, m -> m.move("prefix", "messages.prefix")) // 1 -> 2
                .migration(2, m -> m.set("storage.type", m.has("mysql") ? "mysql" : "sqlite")) // 2 -> 3
                .logger(plugin.getLogger())
                .build(); // old files are backed up as config.yml.v1.<timestamp>.bak before migrating
        // @end
    }

    static void reload(YamlConfig config, CommandSender sender) {
        // @snippet:config.reload
        try {
            config.reload();
            sender.sendMessage("Reloaded.");
        } catch (ConfigException e) {
            sender.sendMessage("Reload failed, keeping the previous settings: " + e.getMessage());
        }
        // @end
    }

    static void save(YamlConfig config) {
        // @snippet:config.save
        config.set("homes.max-homes", 5);
        config.set("spawn.world", "world");
        config.save(); // atomic write; comments and untouched values keep their formatting
        // @end
    }

    static YamlConfig serializer(JavaPlugin plugin) {
        // @snippet:config.serializer
        TypeSerializer<Location> locations = new TypeSerializer<Location>() {
            @Override
            public Location deserialize(Object raw) {
                String[] p = raw.toString().split(",");
                World world = Bukkit.getWorld(p[0]);
                if (world == null) {
                    throw new IllegalArgumentException("unknown world '" + p[0] + "'");
                }
                return new Location(
                        world, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]));
            }

            @Override
            public Object serialize(Location value) {
                return value.getWorld().getName() + "," + value.getX() + "," + value.getY() + "," + value.getZ();
            }
        };
        YamlConfig config = YamlConfig.builder(plugin.getDataFolder().toPath().resolve("spawns.yml"))
                .serializer(Location.class, locations)
                .build();
        Location spawn = config.get("spawn", Location.class);
        // @end
        spawn.hashCode();
        return config;
    }
}
