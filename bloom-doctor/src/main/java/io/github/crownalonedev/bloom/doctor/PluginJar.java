package io.github.crownalonedev.bloom.doctor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** A plugin jar's classes, entry names and plugin.yml. */
final class PluginJar {

    private static final Pattern YAML_KEY =
            Pattern.compile("^(main|name|version)\\s*:\\s*['\"]?([^'\"#]+?)['\"]?\\s*(#.*)?$");

    final File file;
    final Map<String, ClassInfo> classes = new LinkedHashMap<String, ClassInfo>();
    final List<String> entries = new ArrayList<String>();
    final List<String> unreadableClasses = new ArrayList<String>();
    String pluginYml;
    String main;
    String name;
    String version;

    private PluginJar(File file) {
        this.file = file;
    }

    static PluginJar read(File file) throws IOException {
        PluginJar jar = new PluginJar(file);
        ZipFile zip = new ZipFile(file);
        try {
            Enumeration<? extends ZipEntry> all = zip.entries();
            while (all.hasMoreElements()) {
                ZipEntry entry = all.nextElement();
                String entryName = entry.getName();
                jar.entries.add(entryName);
                if (entryName.equals("plugin.yml")) {
                    jar.pluginYml = new String(ClassPath.readAll(zip.getInputStream(entry)), StandardCharsets.UTF_8);
                    jar.parsePluginYml();
                } else if (entryName.endsWith(".class")
                        && !entryName.startsWith("META-INF/")
                        && !entryName.endsWith("module-info.class")) {
                    try {
                        ClassInfo info = ClassInfo.read(ClassPath.readAll(zip.getInputStream(entry)), true);
                        jar.classes.put(info.name, info);
                    } catch (RuntimeException e) {
                        jar.unreadableClasses.add(entryName + " (" + e + ")");
                    }
                }
            }
        } finally {
            zip.close();
        }
        return jar;
    }

    private void parsePluginYml() {
        for (String line : pluginYml.split("\r?\n")) {
            Matcher m = YAML_KEY.matcher(line);
            if (m.matches()) {
                String value = m.group(2).trim();
                if (m.group(1).equals("main")) {
                    main = value;
                } else if (m.group(1).equals("name")) {
                    name = value;
                } else {
                    version = value;
                }
            }
        }
    }

    boolean hasEntryUnder(String prefix) {
        for (String entry : entries) {
            if (entry.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Internal names of classes whose name ends with the given suffix, e.g. {@code /bloom/api/BloomVersion}. */
    List<String> classesEndingWith(String suffix) {
        List<String> result = new ArrayList<String>();
        for (String className : classes.keySet()) {
            if (className.endsWith(suffix)) {
                result.add(className);
            }
        }
        return result;
    }
}
