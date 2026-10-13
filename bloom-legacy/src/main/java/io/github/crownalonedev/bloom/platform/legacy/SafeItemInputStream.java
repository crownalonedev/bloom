package io.github.crownalonedev.bloom.platform.legacy;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.util.io.BukkitObjectInputStream;

/**
 * {@link BukkitObjectInputStream} that only resolves the classes a serialized item consists of. Plain Java
 * deserialization would instantiate any {@code Serializable} class on the server's class path, which lets a crafted
 * item string run "gadget chains" from bundled libraries. Bukkit objects themselves are rebuilt from maps through
 * {@code ConfigurationSerialization}, not through Java serialization, so they need no entry here.
 */
final class SafeItemInputStream extends BukkitObjectInputStream {

    private static final Set<String> ALLOWED = new HashSet<String>(Arrays.asList(
            "org.bukkit.util.io.Wrapper",
            "java.lang.String",
            "java.lang.Number",
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Character",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double"));

    /** Collections used by ItemStack/ItemMeta serialization (HashMap, ArrayList, Guava ImmutableMap forms, ...). */
    private static final String[] ALLOWED_PACKAGES = {"java.util.", "com.google.common.collect."};

    SafeItemInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        if (!isAllowed(desc.getName())) {
            throw new InvalidClassException(desc.getName(), "not allowed in serialized item data");
        }
        return super.resolveClass(desc);
    }

    @Override
    protected Class<?> resolveProxyClass(String[] interfaces) throws IOException {
        throw new InvalidClassException("proxy", "dynamic proxies are not allowed in serialized item data");
    }

    static boolean isAllowed(String className) {
        String name = className;
        if (name.startsWith("[")) {
            int dims = 0;
            while (dims < name.length() && name.charAt(dims) == '[') {
                dims++;
            }
            String element = name.substring(dims);
            if (element.length() == 1) {
                return "ZBCSIJFD".indexOf(element.charAt(0)) >= 0; // primitive arrays
            }
            if (!element.startsWith("L") || !element.endsWith(";")) {
                return false;
            }
            name = element.substring(1, element.length() - 1);
            if (name.equals("java.lang.Object")) {
                return true; // Object[] backs Guava's serialized forms
            }
        }
        if (ALLOWED.contains(name)) {
            return true;
        }
        for (String prefix : ALLOWED_PACKAGES) {
            if (name.startsWith(prefix) && !name.startsWith("java.util.concurrent.")) {
                return true;
            }
        }
        return false;
    }
}
