package io.github.crownalonedev.bloom.doctor;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Class lookup for linkage resolution: the scanned plugin's classes first, then the target API jars, then the JDK
 * running the doctor. Classes are parsed on first use.
 */
final class ClassPath implements Closeable {

    private final Map<String, ClassInfo> scanned;
    private final List<ZipFile> jars = new ArrayList<ZipFile>();
    private final Map<String, ZipFile> index = new HashMap<String, ZipFile>();
    private final Map<String, ClassInfo> cache = new HashMap<String, ClassInfo>();
    private final Map<String, Boolean> missing = new HashMap<String, Boolean>();

    ClassPath(Map<String, ClassInfo> scanned, List<File> apiJars) throws IOException {
        this.scanned = scanned;
        for (File file : apiJars) {
            ZipFile zip = new ZipFile(file);
            jars.add(zip);
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement().getName();
                if (entry.endsWith(".class") && !entry.startsWith("META-INF/")) {
                    String name = entry.substring(0, entry.length() - ".class".length());
                    if (!index.containsKey(name)) {
                        index.put(name, zip);
                    }
                }
            }
        }
    }

    /** Whether the class comes from a target API jar (as opposed to the plugin or the JDK). */
    boolean isApiClass(String name) {
        return !scanned.containsKey(name) && index.containsKey(name);
    }

    int apiClassCount() {
        return index.size();
    }

    /**
     * Finds a class.
     *
     * @param name internal name
     * @return the class, or {@code null} if it is in none of the sources
     */
    ClassInfo find(String name) {
        ClassInfo own = scanned.get(name);
        if (own != null) {
            return own;
        }
        ClassInfo cached = cache.get(name);
        if (cached != null || missing.containsKey(name)) {
            return cached;
        }
        ClassInfo info = null;
        try {
            ZipFile zip = index.get(name);
            if (zip != null) {
                InputStream in = zip.getInputStream(zip.getEntry(name + ".class"));
                info = ClassInfo.read(readAll(in), false);
            } else if (name.startsWith("java/") || name.startsWith("javax/") || name.startsWith("jdk/")) {
                InputStream in = ClassLoader.getSystemResourceAsStream(name + ".class");
                if (in != null) {
                    info = ClassInfo.read(readAll(in), false);
                }
            }
        } catch (IOException | RuntimeException e) {
            info = null;
        }
        if (info == null) {
            missing.put(name, Boolean.TRUE);
        } else {
            cache.put(name, info);
        }
        return info;
    }

    static byte[] readAll(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    @Override
    public void close() throws IOException {
        for (ZipFile zip : jars) {
            zip.close();
        }
    }
}
