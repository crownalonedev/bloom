package io.github.crownalonedev.bloom.api.platform;

/**
 * Describes the Java runtime the server is running on.
 *
 * @since 0.1.0
 */
public final class JavaRuntime {

    private final int featureVersion;
    private final String fullVersion;
    private final String vendor;

    private JavaRuntime(int featureVersion, String fullVersion, String vendor) {
        this.featureVersion = featureVersion;
        this.fullVersion = fullVersion;
        this.vendor = vendor;
    }

    /**
     * Describes the current JVM.
     *
     * @return the current runtime
     */
    public static JavaRuntime current() {
        return of(
                System.getProperty("java.specification.version", "0"),
                System.getProperty("java.version", "unknown"),
                System.getProperty("java.vendor", "unknown"));
    }

    /**
     * Creates a runtime description from system property values.
     *
     * @param specificationVersion value of {@code java.specification.version}, e.g. {@code "1.8"} or {@code "21"}
     * @param fullVersion value of {@code java.version}
     * @param vendor value of {@code java.vendor}
     * @return the runtime description
     */
    public static JavaRuntime of(String specificationVersion, String fullVersion, String vendor) {
        return new JavaRuntime(parseFeatureVersion(specificationVersion), fullVersion, vendor);
    }

    /**
     * Converts a specification version string to a feature version number.
     *
     * @param specificationVersion {@code "1.8"} style (Java 8 and older) or {@code "17"} style
     * @return the feature version, e.g. {@code 8} or {@code 21}; {@code 0} if unparseable
     */
    static int parseFeatureVersion(String specificationVersion) {
        String spec = specificationVersion.trim();
        if (spec.startsWith("1.")) {
            spec = spec.substring(2);
        }
        int dot = spec.indexOf('.');
        if (dot >= 0) {
            spec = spec.substring(0, dot);
        }
        try {
            return Integer.parseInt(spec);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Returns the Java feature version.
     *
     * @return e.g. {@code 8}, {@code 21} or {@code 25}
     */
    public int featureVersion() {
        return featureVersion;
    }

    /**
     * Returns the full version string.
     *
     * @return e.g. {@code "1.8.0_472"} or {@code "21.0.11"}
     */
    public String fullVersion() {
        return fullVersion;
    }

    /**
     * Returns the runtime vendor.
     *
     * @return vendor name
     */
    public String vendor() {
        return vendor;
    }

    /**
     * Returns whether the runtime is at least the given feature version.
     *
     * @param feature feature version to compare with
     * @return {@code true} if {@code featureVersion() >= feature}
     */
    public boolean isAtLeast(int feature) {
        return featureVersion >= feature;
    }

    @Override
    public String toString() {
        return "Java " + fullVersion + " (" + vendor + ")";
    }
}
