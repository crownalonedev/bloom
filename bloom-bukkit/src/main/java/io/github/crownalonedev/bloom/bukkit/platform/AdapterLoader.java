package io.github.crownalonedev.bloom.bukkit.platform;

import io.github.crownalonedev.bloom.api.BloomStartupException;
import io.github.crownalonedev.bloom.api.platform.PlatformFamily;
import io.github.crownalonedev.bloom.api.platform.PlatformInfo;

/**
 * Loads the adapter for a platform family by class name.
 *
 * <p>The class name is derived from the <em>runtime</em> package of this class, so it stays correct when plugins
 * relocate Bloom (e.g. to {@code com.example.libs.bloom}). Only the selected family's class is ever loaded, which
 * keeps Java 21 adapter bytecode away from Java 8 servers.
 *
 * @since 0.1.0
 */
public final class AdapterLoader {

    private AdapterLoader() {}

    /**
     * Returns the fully qualified adapter class name for a family, relative to Bloom's runtime root package.
     *
     * @param family platform family
     * @return class name, e.g. {@code io.github.crownalonedev.bloom.platform.modern.ModernPlatformAdapter}
     */
    public static String adapterClassName(PlatformFamily family) {
        return adapterClassName(AdapterLoader.class.getName(), family);
    }

    static String adapterClassName(String loaderClassName, PlatformFamily family) {
        String suffix = ".bukkit.platform.AdapterLoader";
        if (!loaderClassName.endsWith(suffix)) {
            throw new IllegalStateException("Unexpected Bloom class layout: " + loaderClassName);
        }
        String root = loaderClassName.substring(0, loaderClassName.length() - suffix.length());
        String simple =
                Character.toUpperCase(family.id().charAt(0)) + family.id().substring(1);
        return root + ".platform." + family.id() + "." + simple + "PlatformAdapter";
    }

    /**
     * Loads and instantiates an adapter.
     *
     * @param family selected family
     * @param platform detected platform, used in error messages
     * @return a new adapter instance (not yet enabled)
     * @throws BloomStartupException if the adapter is missing or cannot be loaded, with a corrective message
     */
    public static PlatformAdapter load(PlatformFamily family, PlatformInfo platform) {
        String className = adapterClassName(family);
        Class<?> type;
        try {
            type = Class.forName(className, true, AdapterLoader.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new BloomStartupException(
                    "This server (" + platform.describe() + ") needs the Bloom " + family.id()
                            + " adapter, but it is not in the plugin jar (class " + className + " not found). Add the "
                            + "dependency 'io.github.crownalonedev.bloom:" + family.adapterModule()
                            + "' and make sure your "
                            + "shadow/shade configuration includes it.",
                    e);
        } catch (UnsupportedClassVersionError e) {
            throw new BloomStartupException(
                    "The Bloom " + family.id() + " adapter was compiled for a newer Java "
                            + "version than this server runs (" + platform.javaRuntime() + ").",
                    e);
        } catch (LinkageError e) {
            throw new BloomStartupException(
                    "The Bloom " + family.id() + " adapter failed to link on " + platform.describe()
                            + ". This usually means the adapter does not match the server API.",
                    e);
        }
        if (!PlatformAdapter.class.isAssignableFrom(type)) {
            throw new BloomStartupException(className + " does not implement PlatformAdapter. Is the jar built "
                    + "from mismatched Bloom versions?");
        }
        try {
            return (PlatformAdapter) type.getConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new BloomStartupException(
                    "Could not instantiate the Bloom " + family.id() + " adapter on " + platform.describe() + ".", e);
        }
    }
}
