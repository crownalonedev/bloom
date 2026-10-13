package io.github.crownalonedev.bloomexamples.docs;

import io.github.crownalonedev.bloom.api.BloomStartupException;
import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.api.platform.MinecraftVersion;
import io.github.crownalonedev.bloom.api.text.TextFormat;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.bukkit.BloomOptions;
import io.github.crownalonedev.bloom.bukkit.UnsupportedFeaturePolicy;
import org.bukkit.plugin.java.JavaPlugin;

/** Core lifecycle snippets used in the documentation. */
final class CoreSnippets {

    private CoreSnippets() {}

    static final class OptionsPlugin extends JavaPlugin {
        private Bloom bloom;

        @Override
        public void onEnable() {
            // @snippet:core.options
            bloom = Bloom.enable(
                    this,
                    BloomOptions.builder()
                            .textFormat(
                                    TextFormat.MINIMESSAGE) // default format for messages, item names and menu titles
                            .unsupportedFeaturePolicy(UnsupportedFeaturePolicy.FALLBACK_TO_CHAT)
                            .debug(false)
                            .build());
            // @end
        }

        @Override
        public void onDisable() {
            if (bloom != null) {
                bloom.shutdown();
            }
        }
    }

    static final class GracefulPlugin extends JavaPlugin {
        private Bloom bloom;

        // @snippet:core.graceful
        @Override
        public void onEnable() {
            try {
                bloom = Bloom.enable(this);
            } catch (BloomStartupException e) {
                // The message names the server, the reason and the fix; Bloom has already logged it.
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            // ... the rest of your startup
        }

        @Override
        public void onDisable() {
            if (bloom != null) {
                bloom.shutdown();
            }
        }
        // @end
    }

    static void capabilities(Bloom bloom) {
        // @snippet:core.capabilities
        if (bloom.has(Capability.BOSS_BAR)) {
            // Paper: show a boss bar
        } else {
            // 1.7 / 1.8: use the scoreboard or chat instead
        }

        MinecraftVersion version = bloom.platform().minecraftVersion();
        if (version.isAtLeast(MinecraftVersion.V1_21_1)) {
            // only reached on modern Paper
        }
        // @end
    }

    interface Economy {
        double balance(java.util.UUID player);
    }

    static void services(Bloom bloom, Economy myEconomy) {
        // @snippet:core.services
        bloom.services().register(Economy.class, myEconomy); // closed on shutdown if it is AutoCloseable
        Economy economy = bloom.service(Economy.class); // throws a descriptive error if missing
        // @end
        economy.hashCode();
    }
}
