package io.github.crownalonedev.bloom.doctor;

import java.util.ArrayList;
import java.util.List;

/** Packaging checks that need only the jar: plugin.yml, relocation, adapters and class file versions. */
final class JarChecks {

    static final String BLOOM = "io/github/crownalonedev/bloom/";
    static final String LEGACY_ADAPTER = "/bloom/platform/legacy/LegacyPlatformAdapter";
    static final String MODERN_ADAPTER = "/bloom/platform/modern/ModernPlatformAdapter";
    static final String MODERN_PACKAGE = "/bloom/platform/modern/";

    /** Third-party libraries Bloom modules bundle, which must be relocated too. */
    private static final String[][] BUNDLED = {
        {
            "org/yaml/snakeyaml/",
            "org.yaml.snakeyaml.",
            "snakeyaml",
            "bloom-config bundles SnakeYAML 2.x; servers ship "
                    + "their own (1.15 on 1.8.8), and the wrong one would be loaded."
        },
        {
            "com/zaxxer/hikari/",
            "com.zaxxer.hikari.",
            "hikari",
            "bloom-storage bundles HikariCP; other plugins may ship a " + "different version."
        },
        {"org/slf4j/", "org.slf4j.", "slf4j", "HikariCP brings SLF4J; servers and other plugins ship other versions."},
    };

    private JarChecks() {}

    /** Result of the packaging checks, including what the linkage step needs to know. */
    static final class Result {
        final List<Finding> findings = new ArrayList<Finding>();
        boolean legacyAdapter;
        boolean modernAdapter;
    }

    static Result run(PluginJar jar) {
        Result result = new Result();
        List<Finding> out = result.findings;

        if (jar.pluginYml == null) {
            out.add(Finding.error(
                    "No plugin.yml in the jar.",
                    "Put plugin.yml in src/main/resources (Bukkit will not load the jar without it)."));
        } else if (jar.main == null) {
            out.add(Finding.error("plugin.yml has no 'main' entry.", "Add main: <your plugin class>."));
        } else if (!jar.classes.containsKey(jar.main.replace('.', '/'))) {
            out.add(Finding.error(
                    "plugin.yml main class " + jar.main + " is not in the jar.",
                    "Check the class name and package; if it was relocated, your plugin code is inside a relocated "
                            + "package. Keep your code outside io.github.crownalonedev.bloom and end relocation "
                            + "patterns with '.'."));
        } else {
            out.add(Finding.ok("plugin.yml: " + (jar.name == null ? "?" : jar.name) + " "
                    + (jar.version == null ? "" : jar.version) + " (main " + jar.main + ")"));
        }

        if (jar.hasEntryUnder("org/bukkit/") || jar.hasEntryUnder("net/md_5/bungee/api/")) {
            out.add(Finding.error(
                    "The jar contains the Bukkit/Spigot API.",
                    "Declare spigot-api / paper-api as compileOnly, not implementation."));
        }

        List<String> versions = jar.classesEndingWith("/bloom/api/BloomVersion");
        boolean bloomPresent = !versions.isEmpty();
        if (!bloomPresent) {
            out.add(Finding.info("Bloom is not embedded in this jar; Bloom checks skipped."));
        } else {
            if (jar.hasEntryUnder(BLOOM)) {
                out.add(Finding.error(
                        "Bloom is not relocated (classes under io.github.crownalonedev.bloom).",
                        "In shadowJar: relocate(\"io.github.crownalonedev.bloom.\", \"<your.package>.libs.bloom.\"). "
                                + "Without it, two plugins with different Bloom versions break each other."));
            }
            for (String versionClass : versions) {
                Object version = jar.classes.get(versionClass).constants.get("VERSION");
                String root = versionClass.substring(0, versionClass.length() - "/api/BloomVersion".length());
                out.add(Finding.info("Bloom " + version + " at " + root.replace('/', '.')));
            }
            if (versions.size() > 1) {
                out.add(Finding.warning(
                        "The jar contains " + versions.size() + " copies of Bloom.",
                        "Shade Bloom once; check for a dependency that already bundles it."));
            }

            result.legacyAdapter = !jar.classesEndingWith(LEGACY_ADAPTER).isEmpty();
            result.modernAdapter = !jar.classesEndingWith(MODERN_ADAPTER).isEmpty();
            if (!result.legacyAdapter && !result.modernAdapter) {
                out.add(Finding.error(
                        "No Bloom platform adapter in the jar: Bloom will refuse to start on every server.",
                        "Add runtimeOnly(\"io.github.crownalonedev.bloom:bloom-legacy\") for 1.7.10 - 1.8.9 and/or "
                                + "bloom-modern for Paper 1.21.1 - 26.3.x. Do not use shadowJar.minimize()."));
            } else {
                out.add(Finding.ok("Adapters: "
                        + (result.legacyAdapter ? "legacy (1.7.10 - 1.8.9)" : "")
                        + (result.legacyAdapter && result.modernAdapter ? ", " : "")
                        + (result.modernAdapter ? "modern (Paper 1.21.1 - 26.3.x)" : "")));
            }

            for (String[] lib : BUNDLED) {
                if (jar.hasEntryUnder(lib[0])) {
                    out.add(Finding.error(
                            lib[1].substring(0, lib[1].length() - 1) + " is bundled but not relocated. " + lib[3],
                            "In shadowJar: relocate(\"" + lib[1] + "\", \"<your.package>.libs." + lib[2] + ".\")"));
                }
            }
        }

        checkClassVersions(jar, result);
        if (!jar.unreadableClasses.isEmpty()) {
            out.add(Finding.warning(
                    jar.unreadableClasses.size() + " class file(s) could not be read, e.g. "
                            + jar.unreadableClasses.get(0),
                    "They were skipped by the linkage check."));
        }
        return result;
    }

    /**
     * Legacy servers run Java 8 and Paper 1.21.x runs Java 21: classes outside Bloom's modern adapter must not be
     * newer than the oldest targeted server's Java.
     */
    private static void checkClassVersions(PluginJar jar, Result result) {
        int highest = 0;
        String highestClass = null;
        for (ClassInfo type : jar.classes.values()) {
            if (type.name.contains(MODERN_PACKAGE)) {
                continue;
            }
            if (type.javaVersion() > highest) {
                highest = type.javaVersion();
                highestClass = type.name.replace('/', '.');
            }
        }
        if (highestClass == null) {
            return;
        }
        boolean targetsLegacy = result.legacyAdapter || (!result.modernAdapter && highest <= 8);
        if (targetsLegacy && highest > 8) {
            result.findings.add(Finding.error(
                    "Classes need Java " + highest + " (e.g. " + highestClass + "), but 1.7.10 - 1.8.9 servers run "
                            + "Java 8.",
                    "Compile with options.release = 8, or drop bloom-legacy if you only target modern Paper."));
        } else if (result.modernAdapter && highest > 21) {
            result.findings.add(Finding.warning(
                    "Classes need Java " + highest + " (e.g. " + highestClass + "); Paper 1.21.x servers run Java 21.",
                    "Compile with options.release = 21 unless you only target 26.x."));
        } else {
            result.findings.add(Finding.ok("Bytecode: Java " + highest + " (outside the modern adapter)"));
        }
    }
}
