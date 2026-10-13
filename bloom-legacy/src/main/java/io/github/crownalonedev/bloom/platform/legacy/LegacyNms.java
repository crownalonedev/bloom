package io.github.crownalonedev.bloom.platform.legacy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Reflective access to the few CraftBukkit/NMS members Bloom needs on 1.7.10 - 1.8.x: action bar packets and item
 * NBT. Everything is resolved once at startup; a feature is only enabled when every member it needs was found, so
 * a mismatch disables the capability instead of failing at use time.
 *
 * <p>Member names are the Spigot-mapped names that are stable across v1_7_R4 and v1_8_R1-R3.
 */
final class LegacyNms {

    static final String BLOOM_COMPOUND = "BloomData";

    private final String version;

    private boolean actionBarReady;
    private Method chatSerializerA;
    private Constructor<?> packetChat;
    private Method getHandle;
    private Field playerConnection;
    private Method sendPacket;

    private boolean nbtReady;
    private Method asNmsCopy;
    private Method asBukkitCopy;
    private Method getTag;
    private Method setTag;
    private Constructor<?> newCompound;
    private Method hasKey;
    private Method getCompound;
    private Method set;
    private Method setString;
    private Method getString;
    private Method remove;
    private Method isEmpty;

    private LegacyNms(String version) {
        this.version = version;
    }

    /**
     * Resolves NMS members for the running server.
     *
     * @param server running server
     * @param logger receives probe details when {@code debug} is set
     * @param debug whether to log probe failures
     * @return the access object; check {@link #actionBarReady()} and {@link #nbtReady()}
     */
    static LegacyNms probe(Server server, Logger logger, boolean debug) {
        String version = versionPackage(server.getClass().getName());
        LegacyNms nms = new LegacyNms(version);
        if (version == null) {
            if (debug) {
                logger.info("[Bloom] No versioned CraftBukkit package found ("
                        + server.getClass().getName() + "); NMS features disabled.");
            }
            return nms;
        }
        try {
            nms.resolveActionBar();
            nms.actionBarReady = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (debug) {
                logger.info("[Bloom] Action bar probe failed on " + version + ": " + e);
            }
        }
        try {
            nms.resolveNbt();
            nms.nbtReady = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (debug) {
                logger.info("[Bloom] Item NBT probe failed on " + version + ": " + e);
            }
        }
        return nms;
    }

    /**
     * Extracts the version segment of the CraftServer class name.
     *
     * @param craftServerClass e.g. {@code org.bukkit.craftbukkit.v1_8_R3.CraftServer}
     * @return e.g. {@code v1_8_R3}, or {@code null} if the package is not versioned
     */
    static String versionPackage(String craftServerClass) {
        String prefix = "org.bukkit.craftbukkit.";
        if (!craftServerClass.startsWith(prefix)) {
            return null;
        }
        String rest = craftServerClass.substring(prefix.length());
        int dot = rest.indexOf('.');
        if (dot < 0) {
            return null;
        }
        String segment = rest.substring(0, dot);
        return segment.matches("v\\d+_\\d+_R\\d+") ? segment : null;
    }

    private Class<?> nms(String simpleName) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + version + "." + simpleName);
    }

    private Class<?> craft(String relativeName) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + version + "." + relativeName);
    }

    private void resolveActionBar() throws ReflectiveOperationException {
        Class<?> component = nms("IChatBaseComponent");
        Class<?> serializer;
        try {
            serializer = nms("IChatBaseComponent$ChatSerializer"); // 1.8.3+
        } catch (ClassNotFoundException e) {
            serializer = nms("ChatSerializer"); // 1.7.10 / 1.8.0
        }
        chatSerializerA = serializer.getMethod("a", String.class);
        packetChat = nms("PacketPlayOutChat").getConstructor(component, byte.class);
        getHandle = craft("entity.CraftPlayer").getMethod("getHandle");
        playerConnection = getHandle.getReturnType().getField("playerConnection");
        sendPacket = playerConnection.getType().getMethod("sendPacket", nms("Packet"));
    }

    private void resolveNbt() throws ReflectiveOperationException {
        Class<?> craftItemStack = craft("inventory.CraftItemStack");
        asNmsCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
        Class<?> nmsItemStack = asNmsCopy.getReturnType();
        asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", nmsItemStack);
        Class<?> compound = nms("NBTTagCompound");
        Class<?> base = nms("NBTBase");
        getTag = nmsItemStack.getMethod("getTag");
        setTag = nmsItemStack.getMethod("setTag", compound);
        newCompound = compound.getConstructor();
        hasKey = compound.getMethod("hasKey", String.class);
        getCompound = compound.getMethod("getCompound", String.class);
        set = compound.getMethod("set", String.class, base);
        setString = compound.getMethod("setString", String.class, String.class);
        getString = compound.getMethod("getString", String.class);
        remove = compound.getMethod("remove", String.class);
        try {
            isEmpty = compound.getMethod("isEmpty");
        } catch (NoSuchMethodException e) {
            isEmpty = null; // optional: only used to drop an empty BloomData compound
        }
    }

    boolean actionBarReady() {
        return actionBarReady;
    }

    boolean nbtReady() {
        return nbtReady;
    }

    String version() {
        return version;
    }

    void sendActionBar(Player player, String legacyText) {
        try {
            Object component = chatSerializerA.invoke(null, "{\"text\":" + jsonString(legacyText) + "}");
            Object packet = packetChat.newInstance(component, (byte) 2);
            Object connection = playerConnection.get(getHandle.invoke(player));
            sendPacket.invoke(connection, packet);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to send action bar on " + version, e);
        }
    }

    ItemStack writeString(ItemStack item, String key, String value) {
        try {
            Object nmsItem = requireNmsCopy(item);
            Object tag = getTag.invoke(nmsItem);
            if (tag == null) {
                tag = newCompound.newInstance();
                setTag.invoke(nmsItem, tag);
            }
            Object data = getCompound.invoke(tag, BLOOM_COMPOUND);
            setString.invoke(data, key, value);
            set.invoke(tag, BLOOM_COMPOUND, data);
            return (ItemStack) asBukkitCopy.invoke(null, nmsItem);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to write item NBT on " + version, e);
        }
    }

    Optional<String> readString(ItemStack item, String key) {
        try {
            Object nmsItem = asNmsCopy.invoke(null, item);
            if (nmsItem == null) {
                return Optional.empty();
            }
            Object tag = getTag.invoke(nmsItem);
            if (tag == null || !(Boolean) hasKey.invoke(tag, BLOOM_COMPOUND)) {
                return Optional.empty();
            }
            Object data = getCompound.invoke(tag, BLOOM_COMPOUND);
            if (!(Boolean) hasKey.invoke(data, key)) {
                return Optional.empty();
            }
            return Optional.of((String) getString.invoke(data, key));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read item NBT on " + version, e);
        }
    }

    ItemStack removeString(ItemStack item, String key) {
        try {
            Object nmsItem = asNmsCopy.invoke(null, item);
            if (nmsItem == null) {
                return item.clone();
            }
            Object tag = getTag.invoke(nmsItem);
            if (tag == null || !(Boolean) hasKey.invoke(tag, BLOOM_COMPOUND)) {
                return item.clone();
            }
            Object data = getCompound.invoke(tag, BLOOM_COMPOUND);
            remove.invoke(data, key);
            if (isEmpty != null && (Boolean) isEmpty.invoke(data)) {
                remove.invoke(tag, BLOOM_COMPOUND);
            } else {
                set.invoke(tag, BLOOM_COMPOUND, data);
            }
            return (ItemStack) asBukkitCopy.invoke(null, nmsItem);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to remove item NBT on " + version, e);
        }
    }

    private Object requireNmsCopy(ItemStack item) throws ReflectiveOperationException {
        Object nmsItem = asNmsCopy.invoke(null, item);
        if (nmsItem == null) {
            throw new IllegalArgumentException("Cannot store data on an empty item (" + item.getType() + ").");
        }
        return nmsItem;
    }

    /**
     * Encodes a string as a JSON string literal.
     *
     * @param text raw text
     * @return quoted, escaped JSON string
     */
    static String jsonString(String text) {
        StringBuilder out = new StringBuilder(text.length() + 2).append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
    }
}
