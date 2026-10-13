package io.github.crownalonedev.bloomexamples.crossversion;

import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.api.platform.PlatformFamily;
import io.github.crownalonedev.bloom.api.text.Placeholder;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.bukkit.BloomOptions;
import io.github.crownalonedev.bloom.bukkit.UnsupportedFeaturePolicy;
import io.github.crownalonedev.bloom.bukkit.text.BloomBossBar;
import io.github.crownalonedev.bloom.bukkit.text.BossBarColor;
import io.github.crownalonedev.bloom.bukkit.text.BossBarStyle;
import io.github.crownalonedev.bloom.bukkit.text.Messenger;
import io.github.crownalonedev.bloom.bukkit.text.Title;
import io.github.crownalonedev.bloom.gui.Button;
import io.github.crownalonedev.bloom.gui.Menu;
import io.github.crownalonedev.bloom.gui.Menus;
import io.github.crownalonedev.bloom.items.Items;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * One jar for 1.7.10 to 26.x. Shows the three ways to deal with features that only some versions have:
 *
 * <ol>
 *   <li>check a capability and branch ({@link Bloom#has(Capability)})
 *   <li>let Bloom fall back automatically ({@link UnsupportedFeaturePolicy#FALLBACK_TO_CHAT})
 *   <li>use version-neutral names (modern material names, which Bloom maps on legacy servers)
 * </ol>
 */
public final class CrossVersionExamplePlugin extends JavaPlugin {

    private Bloom bloom;
    private Messenger messages;
    private BloomBossBar bossBar;

    @Override
    public void onEnable() {
        bloom = Bloom.enable(
                this,
                BloomOptions.builder()
                        .unsupportedFeaturePolicy(UnsupportedFeaturePolicy.FALLBACK_TO_CHAT)
                        .build());
        messages = bloom.messages().withPrefix("&d[Bloom] &7");
        if (bloom.has(Capability.BOSS_BAR)) {
            bossBar = bloom.messages().bossBar("&dBloom is running", 1.0f, BossBarColor.PINK, BossBarStyle.SOLID);
        }
        getCommand("bloominfo").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        messages.send(
                sender,
                "Platform: &f{platform}",
                Placeholder.of("platform", bloom.platform().describe()));
        messages.send(
                sender,
                "Adapter: &f{adapter}",
                Placeholder.of("adapter", bloom.adapter().description()));
        messages.send(
                sender,
                "Available: &a{caps}",
                Placeholder.of("caps", bloom.capabilities().available()));
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        // 2) Falls back to a chat line on servers without titles/action bars (FALLBACK_TO_CHAT policy above).
        messages.title(
                player,
                Title.of("&dBloom", "&7{family} platform"),
                Placeholder.of("family", bloom.adapter().family().id()));
        messages.actionBar(player, "&7Action bar or chat - Bloom decides.");
        // 1) Explicit capability check.
        if (bossBar != null) {
            bossBar.show(player);
        }
        Menus.of(bloom).open(player, capabilityMenu());
        return true;
    }

    private Menu capabilityMenu() {
        Items items = Items.of(bloom);
        List<Button> rows = new ArrayList<Button>();
        for (Capability capability : Capability.values()) {
            boolean available = bloom.has(capability);
            // 3) Modern names everywhere: LIME_WOOL becomes WOOL:5 on 1.8.
            ItemStack icon = items.builder(available ? "LIME_WOOL" : "RED_WOOL")
                    .name((available ? "&a" : "&c") + capability.name())
                    .lore("&7" + capability.description())
                    .build();
            rows.add(Button.of(icon));
        }
        boolean legacy = bloom.adapter().family() == PlatformFamily.LEGACY;
        return Menu.builder()
                .title(legacy ? "&dCapabilities" : "&dCapabilities &8({page}/{pages})")
                .rows(3)
                .paginate(view -> rows)
                .build();
    }

    @Override
    public void onDisable() {
        if (bloom != null) {
            bloom.shutdown(); // also hides the boss bar
        }
    }
}
