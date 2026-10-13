package io.github.crownalonedev.bloom.platform.legacy;

import io.github.crownalonedev.bloom.api.platform.Capabilities;
import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.api.platform.MinecraftVersion;
import io.github.crownalonedev.bloom.api.platform.PlatformFamily;
import io.github.crownalonedev.bloom.bukkit.platform.AdapterContext;
import io.github.crownalonedev.bloom.bukkit.platform.ItemPlatform;
import io.github.crownalonedev.bloom.bukkit.platform.PlatformAdapter;
import io.github.crownalonedev.bloom.bukkit.platform.PlayerPlatform;
import io.github.crownalonedev.bloom.bukkit.platform.TextPlatform;
import java.util.EnumSet;

/**
 * Adapter for Bukkit/Spigot/PaperSpigot 1.7.10 - 1.8.9 on Java 8.
 *
 * <p>Features that vary inside this range are probed at startup:
 *
 * <ul>
 *   <li>{@code TITLES}: {@code Player#sendTitle(String, String)} (Spigot 1.8.x)
 *   <li>{@code ACTION_BAR}: NMS chat packet with type 2 (1.8 protocol; never on 1.7.10)
 *   <li>{@code ITEM_NBT_TAGS}: CraftItemStack/NBTTagCompound reflection
 *   <li>{@code ITEM_FLAGS}: {@code ItemFlag} (1.8+)
 *   <li>{@code UNBREAKABLE_ITEMS}: {@code ItemMeta.Spigot#setUnbreakable}
 * </ul>
 *
 * <p>Loaded reflectively by {@code AdapterLoader}; do not reference this class from shared code.
 */
public final class LegacyPlatformAdapter implements PlatformAdapter {

    private LegacyTextPlatform text;
    private LegacyPlayers players;
    private LegacyItems items;

    /** Public no-argument constructor required by the adapter loader. */
    public LegacyPlatformAdapter() {}

    @Override
    public PlatformFamily family() {
        return PlatformFamily.LEGACY;
    }

    @Override
    public String description() {
        return "legacy (Bukkit 1.7.10-1.8.9)";
    }

    @Override
    public Capabilities enable(AdapterContext context) {
        LegacyNms nms = LegacyNms.probe(context.plugin().getServer(), context.logger(), context.debug());
        text = new LegacyTextPlatform(nms);
        players = new LegacyPlayers(context.logger());
        items = new LegacyItems(nms, LegacyProbes.itemFlags());

        boolean protocol17 = context.platform().minecraftVersion().isSameLine(MinecraftVersion.of(1, 7));
        EnumSet<Capability> present = EnumSet.of(Capability.LEGACY_DATA_VALUES);
        if (LegacyProbes.titles() && !protocol17) {
            present.add(Capability.TITLES);
        }
        if (nms.actionBarReady() && !protocol17) {
            present.add(Capability.ACTION_BAR);
        }
        if (nms.nbtReady()) {
            present.add(Capability.ITEM_NBT_TAGS);
        }
        if (LegacyProbes.itemFlags()) {
            present.add(Capability.ITEM_FLAGS);
        }
        if (LegacyProbes.unbreakable()) {
            present.add(Capability.UNBREAKABLE_ITEMS);
        }
        if (context.debug()) {
            context.logger()
                    .info("[Bloom] Legacy probes: nms=" + nms.version() + ", actionBar=" + nms.actionBarReady()
                            + ", nbt=" + nms.nbtReady() + ", titles=" + LegacyProbes.titles() + ", itemFlags="
                            + LegacyProbes.itemFlags() + ", unbreakable=" + LegacyProbes.unbreakable());
        }
        return Capabilities.of(present, context.platform().describe());
    }

    @Override
    public void disable() {
        // Nothing to release: reflection handles are plain references.
    }

    @Override
    public TextPlatform text() {
        return text;
    }

    @Override
    public PlayerPlatform players() {
        return players;
    }

    @Override
    public ItemPlatform items() {
        return items;
    }
}
