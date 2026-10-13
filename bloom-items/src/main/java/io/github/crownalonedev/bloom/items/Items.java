package io.github.crownalonedev.bloom.items;

import io.github.crownalonedev.bloom.api.UnsupportedCapabilityException;
import io.github.crownalonedev.bloom.api.platform.Capabilities;
import io.github.crownalonedev.bloom.api.platform.Capability;
import io.github.crownalonedev.bloom.bukkit.Bloom;
import io.github.crownalonedev.bloom.bukkit.UnsupportedFeaturePolicy;
import io.github.crownalonedev.bloom.bukkit.platform.ItemPlatform;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Cross-version item creation, identification and serialization.
 *
 * <pre>{@code
 * Items items = Items.of(bloom);
 *
 * ItemStack sword = items.builder("DIAMOND_SWORD")
 *         .name("&dBlossom Blade")
 *         .lore("&7Forged under falling petals.")
 *         .enchant("sharpness", 3)
 *         .flags(ItemFlagKey.HIDE_ENCHANTS)
 *         .unbreakable(true)
 *         .build();
 *
 * CustomItem ruby = items.define("ruby", items.builder("RED_DYE").name("&cRuby").glow());
 * player.getInventory().addItem(ruby.create(3));
 * boolean isRuby = ruby.matches(player.getItemInHand());
 * }</pre>
 *
 * <h2>Identity</h2>
 * Custom ids and data are stored as namespaced persistent data, never in names or lore: PersistentDataContainer on
 * modern servers, NBT on legacy servers. The namespace is the owning plugin's lowercase name. Data written on one
 * platform family cannot be read on the other.
 *
 * @since 0.1.0
 */
public final class Items {

    /** Data key used for custom item ids. */
    public static final String ID_KEY = "item_id";

    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z0-9._-]+");

    private final Bloom bloom;
    private final ItemPlatform platform;
    private final MaterialMappings materials = new MaterialMappings();
    private final String namespace;
    private final Map<String, CustomItem> custom = Collections.synchronizedMap(new LinkedHashMap<String, CustomItem>());
    private final Set<Capability> warned = Collections.synchronizedSet(EnumSet.noneOf(Capability.class));

    private Items(Bloom bloom) {
        this.bloom = bloom;
        this.platform = bloom.adapter().items();
        this.namespace = namespaceOf(bloom.plugin().getName());
    }

    /**
     * Returns the items service of a Bloom instance, creating it on first use.
     *
     * @param bloom running Bloom instance
     * @return the service
     */
    public static Items of(final Bloom bloom) {
        Objects.requireNonNull(bloom, "bloom");
        return bloom.services().getOrRegister(Items.class, () -> new Items(bloom));
    }

    static String namespaceOf(String pluginName) {
        String lower = pluginName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return lower.isEmpty() ? "bloom" : lower;
    }

    /**
     * Starts building an item.
     *
     * @param materialKey modern material name (legacy names and {@code NAME:DATA} accepted); see
     *     {@link MaterialMappings}
     * @return a builder
     * @throws IllegalArgumentException if the material does not exist on this server
     */
    public ItemBuilder builder(String materialKey) {
        return new ItemBuilder(this, require(materialKey, 1));
    }

    /**
     * Starts building from a Bukkit material constant of the API you compile against.
     *
     * @param material material
     * @return a builder
     */
    public ItemBuilder builder(Material material) {
        return new ItemBuilder(this, new ItemStack(material, 1));
    }

    /**
     * Starts editing a copy of an existing item. Name and lore are kept unless the builder sets them.
     *
     * @param item item to copy
     * @return a builder
     */
    public ItemBuilder edit(ItemStack item) {
        return new ItemBuilder(this, item.clone());
    }

    /**
     * Creates a plain item.
     *
     * @param materialKey material key
     * @param amount stack size
     * @return the item, or empty if the material does not exist on this server
     */
    public Optional<ItemStack> create(String materialKey, int amount) {
        boolean legacy = capabilities().has(Capability.LEGACY_DATA_VALUES);
        for (MaterialMappings.Candidate candidate : materials.candidates(materialKey, legacy)) {
            Optional<ItemStack> item = platform.create(candidate.name(), candidate.data(), amount);
            if (item.isPresent()) {
                return item;
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether a material key resolves on this server.
     *
     * @param materialKey material key
     * @return {@code true} if {@link #create(String, int)} would succeed
     */
    public boolean exists(String materialKey) {
        return create(materialKey, 1).isPresent();
    }

    ItemStack require(String materialKey, int amount) {
        Optional<ItemStack> item = create(materialKey, amount);
        if (!item.isPresent()) {
            boolean legacy = capabilities().has(Capability.LEGACY_DATA_VALUES);
            throw new IllegalArgumentException("Unknown material '" + materialKey + "' on "
                    + bloom.platform().describe() + ". Tried: " + materials.candidates(materialKey, legacy)
                    + ". Use a modern material name, or register a mapping with "
                    + "Items.of(bloom).materials().register(...).");
        }
        return item.get();
    }

    /**
     * Returns the material mappings, where custom keys can be registered.
     *
     * @return mappings
     */
    public MaterialMappings materials() {
        return materials;
    }

    /**
     * Returns the persistent data namespace (lowercase plugin name).
     *
     * @return namespace
     */
    public String namespace() {
        return namespace;
    }

    // ---- persistent data ----------------------------------------------------------------------------------

    /**
     * Returns whether items can carry persistent data on this server.
     *
     * @return {@code true} with PersistentDataContainer (modern) or NBT access (legacy)
     */
    public boolean supportsData() {
        Capabilities caps = capabilities();
        return caps.has(Capability.PERSISTENT_DATA_CONTAINER) || caps.has(Capability.ITEM_NBT_TAGS);
    }

    /**
     * Returns a copy of an item with a string value stored under this plugin's namespace.
     *
     * @param item source item (unchanged)
     * @param key key, {@code [a-z0-9._-]+}
     * @param value value
     * @return tagged copy
     * @throws UnsupportedCapabilityException if items cannot carry data on this server
     */
    public ItemStack setData(ItemStack item, String key, String value) {
        requireData("Items.setData");
        return platform.writeData(item, namespace, checkKey(key), Objects.requireNonNull(value, "value"));
    }

    /**
     * Reads a value stored by {@link #setData}.
     *
     * @param item item, may be {@code null}
     * @param key key
     * @return the value, or empty
     */
    public Optional<String> getData(ItemStack item, String key) {
        if (item == null || item.getType() == Material.AIR || !supportsData()) {
            return Optional.empty();
        }
        return platform.readData(item, namespace, checkKey(key));
    }

    /**
     * Returns a copy of an item without a stored value.
     *
     * @param item source item (unchanged)
     * @param key key
     * @return copy without the key
     */
    public ItemStack removeData(ItemStack item, String key) {
        requireData("Items.removeData");
        return platform.removeData(item, namespace, checkKey(key));
    }

    /**
     * Returns a copy of an item marked with a custom id.
     *
     * @param item source item
     * @param id custom id, {@code [a-z0-9._-]+}
     * @return tagged copy
     */
    public ItemStack tag(ItemStack item, String id) {
        return setData(item, ID_KEY, checkKey(id));
    }

    /**
     * Returns the custom id of an item.
     *
     * @param item item, may be {@code null}
     * @return id, or empty if the item has none
     */
    public Optional<String> customId(ItemStack item) {
        return getData(item, ID_KEY);
    }

    /**
     * Returns whether an item carries a custom id.
     *
     * @param item item, may be {@code null}
     * @param id expected id
     * @return {@code true} if the ids match
     */
    public boolean isCustom(ItemStack item, String id) {
        Optional<String> actual = customId(item);
        return actual.isPresent() && actual.get().equals(id);
    }

    // ---- custom item registry -----------------------------------------------------------------------------

    /**
     * Defines a custom item. The template is built once and tagged with the id; {@link CustomItem#create()} returns
     * copies.
     *
     * @param id unique id, {@code [a-z0-9._-]+}
     * @param template builder describing the item
     * @return the definition
     * @throws IllegalStateException if the id is already defined
     */
    public CustomItem define(String id, ItemBuilder template) {
        checkKey(id);
        CustomItem item =
                new CustomItem(this, id, template.customId(id).amount(1).build());
        synchronized (custom) {
            if (custom.containsKey(id)) {
                throw new IllegalStateException("Custom item '" + id + "' is already defined.");
            }
            custom.put(id, item);
        }
        return item;
    }

    /**
     * Looks up a custom item definition.
     *
     * @param id custom id
     * @return the definition, or empty
     */
    public Optional<CustomItem> custom(String id) {
        return Optional.ofNullable(custom.get(id));
    }

    /**
     * Finds the custom item definition an item stack belongs to.
     *
     * @param item item, may be {@code null}
     * @return the definition, or empty for plain items and unknown ids
     */
    public Optional<CustomItem> match(ItemStack item) {
        Optional<String> id = customId(item);
        return id.isPresent() ? custom(id.get()) : Optional.<CustomItem>empty();
    }

    /**
     * Returns all custom item definitions.
     *
     * @return snapshot in definition order
     */
    public Collection<CustomItem> customItems() {
        synchronized (custom) {
            return Collections.unmodifiableList(new java.util.ArrayList<CustomItem>(custom.values()));
        }
    }

    // ---- serialization ------------------------------------------------------------------------------------

    /**
     * Serialises an item to a string for storage (databases, configs). The format is platform-family specific.
     *
     * @param item non-empty item
     * @return serialised item
     */
    public String serialize(ItemStack item) {
        return platform.serialize(item);
    }

    /**
     * Deserialises an item created by {@link #serialize(ItemStack)} on the same platform family.
     *
     * @param data serialised item
     * @return the item
     * @throws IllegalArgumentException if malformed or from the other platform family
     */
    public ItemStack deserialize(String data) {
        return platform.deserialize(data);
    }

    // ---- internals ----------------------------------------------------------------------------------------

    Bloom bloom() {
        return bloom;
    }

    ItemPlatform platform() {
        return platform;
    }

    Capabilities capabilities() {
        return bloom.capabilities();
    }

    /**
     * Applies the Bloom unsupported-feature policy for an item feature.
     *
     * @return {@code true} if the feature is available
     */
    boolean feature(Capability capability, String feature) {
        if (capabilities().has(capability)) {
            return true;
        }
        if (bloom.options().unsupportedFeaturePolicy() == UnsupportedFeaturePolicy.THROW) {
            capabilities().require(capability, feature);
        }
        if (warned.add(capability)) {
            bloom.plugin()
                    .getLogger()
                    .warning(feature + " was skipped: " + capability.description() + " are not available on "
                            + bloom.platform().describe() + ". (This warning is shown once.)");
        }
        return false;
    }

    void warn(String message) {
        bloom.plugin().getLogger().warning(message);
    }

    private void requireData(String feature) {
        if (!supportsData()) {
            throw new UnsupportedCapabilityException(
                    Capability.ITEM_NBT_TAGS, bloom.platform().describe(), feature);
        }
    }

    static String checkKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "Invalid item data key '" + key + "': use lowercase letters, digits, '.', '_' or '-'.");
        }
        return key;
    }

    static List<String> flagNames(Collection<ItemFlagKey> flags) {
        List<String> names = new java.util.ArrayList<String>();
        for (ItemFlagKey flag : flags) {
            names.addAll(flag.bukkitNames());
        }
        return names;
    }
}
