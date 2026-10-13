package io.github.crownalonedev.bloom.platform.modern;

import io.github.crownalonedev.bloom.api.platform.Capabilities;
import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.api.platform.PlatformFamily;
import io.github.crownalonedev.bloom.api.platform.ServerSoftware;
import io.github.crownalonedev.bloom.bukkit.platform.AdapterContext;
import io.github.crownalonedev.bloom.bukkit.platform.ItemPlatform;
import io.github.crownalonedev.bloom.bukkit.platform.PlatformAdapter;
import io.github.crownalonedev.bloom.bukkit.platform.PlayerPlatform;
import io.github.crownalonedev.bloom.bukkit.platform.TextPlatform;
import java.util.EnumSet;

/**
 * Adapter for Paper 1.21.1 - 26.3.x (and Paper forks). Uses Adventure, MiniMessage and PersistentDataContainer,
 * all of which are part of the Paper API on every supported version, so no probing is needed.
 *
 * <p>Loaded reflectively by {@code AdapterLoader}; do not reference this class from shared code.
 */
public final class ModernPlatformAdapter implements PlatformAdapter {

    private ModernText text;
    private ModernPlayers players;
    private ModernItems items;

    /** Public no-argument constructor required by the adapter loader. */
    public ModernPlatformAdapter() {}

    @Override
    public PlatformFamily family() {
        return PlatformFamily.MODERN;
    }

    @Override
    public String description() {
        return "modern (Paper 1.21.1-26.3.x, Adventure)";
    }

    @Override
    public Capabilities enable(AdapterContext context) {
        text = new ModernText();
        players = new ModernPlayers(text, context.platform().software() != ServerSoftware.MOCKBUKKIT);
        items = new ModernItems(text);
        EnumSet<Capability> present = EnumSet.of(
                Capability.ADVENTURE_TEXT,
                Capability.MINIMESSAGE,
                Capability.HEX_COLORS,
                Capability.ACTION_BAR,
                Capability.TITLES,
                Capability.TITLE_TIMINGS,
                Capability.BOSS_BAR,
                Capability.COMPONENT_INVENTORY_TITLES,
                Capability.PERSISTENT_DATA_CONTAINER,
                Capability.ITEM_FLAGS,
                Capability.UNBREAKABLE_ITEMS,
                Capability.CUSTOM_MODEL_DATA);
        return Capabilities.of(present, context.platform().describe());
    }

    @Override
    public void disable() {
        // No adapter-level resources: boss bars are closed by Bloom before the adapter is disabled.
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
