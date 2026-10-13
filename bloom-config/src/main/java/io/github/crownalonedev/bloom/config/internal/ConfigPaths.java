package io.github.crownalonedev.bloom.config.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dot-path access to nested {@code Map<String, Object>} trees. Internal. */
public final class ConfigPaths {

    private ConfigPaths() {}

    /**
     * Splits a path.
     *
     * @param path dot-separated path
     * @return segments
     */
    public static String[] split(String path) {
        if (path == null || path.isEmpty() || path.startsWith(".") || path.endsWith(".") || path.contains("..")) {
            throw new IllegalArgumentException("Invalid config path '" + path + "'");
        }
        return path.split("\\.");
    }

    /**
     * Joins a prefix and a child path.
     *
     * @param prefix prefix, may be empty
     * @param child child path
     * @return joined path
     */
    public static String join(String prefix, String child) {
        return prefix.isEmpty() ? child : prefix + "." + child;
    }

    /**
     * Reads a value.
     *
     * @param root tree, may be {@code null}
     * @param path path
     * @return value, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Object get(Map<String, Object> root, String path) {
        if (root == null) {
            return null;
        }
        Object current = root;
        for (String segment : split(path)) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * Writes a value, creating intermediate sections. A {@code null} value removes the key.
     *
     * @param root tree
     * @param path path
     * @param value value
     */
    @SuppressWarnings("unchecked")
    public static void set(Map<String, Object> root, String path, Object value) {
        String[] segments = split(path);
        Map<String, Object> current = root;
        for (int i = 0; i < segments.length - 1; i++) {
            Object next = current.get(segments[i]);
            if (!(next instanceof Map)) {
                if (value == null) {
                    return;
                }
                next = new LinkedHashMap<String, Object>();
                current.put(segments[i], next);
            }
            current = (Map<String, Object>) next;
        }
        if (value == null) {
            current.remove(segments[segments.length - 1]);
        } else {
            current.put(segments[segments.length - 1], value);
        }
    }

    /**
     * Deep-copies a tree of maps and lists, normalising keys to strings.
     *
     * @param value value to copy
     * @return copy
     */
    @SuppressWarnings("unchecked")
    public static Object deepCopy(Object value) {
        if (value instanceof Map) {
            Map<String, Object> copy = new LinkedHashMap<String, Object>();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                copy.put(String.valueOf(entry.getKey()), deepCopy(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<Object>();
            for (Object element : (List<Object>) value) {
                copy.add(deepCopy(element));
            }
            return copy;
        }
        return value;
    }

    /**
     * Copies keys from {@code defaults} that are missing in {@code target} (recursively).
     *
     * @param target tree to complete
     * @param defaults defaults
     * @return number of added leaf values
     */
    @SuppressWarnings("unchecked")
    public static int addMissing(Map<String, Object> target, Map<String, Object> defaults) {
        int added = 0;
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            Object existing = target.get(entry.getKey());
            if (existing == null) {
                target.put(entry.getKey(), deepCopy(entry.getValue()));
                added++;
            } else if (existing instanceof Map && entry.getValue() instanceof Map) {
                added += addMissing((Map<String, Object>) existing, (Map<String, Object>) entry.getValue());
            }
        }
        return added;
    }
}
