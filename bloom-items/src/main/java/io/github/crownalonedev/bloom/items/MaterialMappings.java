package io.github.crownalonedev.bloom.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves Bloom material keys to the material names of the running server.
 *
 * <p><strong>Keys are modern (1.13+) material names</strong>, e.g. {@code RED_WOOL}, {@code PLAYER_HEAD},
 * {@code OAK_PLANKS}. Legacy names ({@code WATCH}) and legacy {@code NAME:DATA} specs ({@code WOOL:14}) are accepted
 * too. Resolution order:
 *
 * <ul>
 *   <li>legacy servers: developer mapping → built-in modern→legacy table → the key itself → {@code NAME:DATA}
 *   <li>modern servers: developer mapping → the key itself → built-in legacy→modern table
 * </ul>
 *
 * <p>So an ambiguous name such as {@code MELON} always means the modern item (the block) on every server.
 *
 * <p>Thread-safe.
 *
 * @since 0.1.0
 */
public final class MaterialMappings {

    /** A concrete material name and legacy data value to try on the running server. */
    public static final class Candidate {
        private final String name;
        private final short data;

        Candidate(String name, short data) {
            this.name = name;
            this.data = data;
        }

        /**
         * Returns the server material name.
         *
         * @return material constant name
         */
        public String name() {
            return name;
        }

        /**
         * Returns the legacy data value.
         *
         * @return data value, {@code 0} on modern servers
         */
        public short data() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Candidate && ((Candidate) o).name.equals(name) && ((Candidate) o).data == data;
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 31 + data;
        }

        @Override
        public String toString() {
            return data == 0 ? name : name + ":" + data;
        }
    }

    private static final Map<String, Candidate> MODERN_TO_LEGACY = new HashMap<String, Candidate>();
    private static final Map<String, String> LEGACY_TO_MODERN = new HashMap<String, String>();

    static {
        for (BuiltinMaterials.Row row : BuiltinMaterials.rows()) {
            MODERN_TO_LEGACY.put(row.modern, new Candidate(row.legacy, row.data));
            LEGACY_TO_MODERN.put(row.legacy + ":" + row.data, row.modern);
            if (row.data == 0 && !LEGACY_TO_MODERN.containsKey(row.legacy)) {
                LEGACY_TO_MODERN.put(row.legacy, row.modern);
            }
        }
    }

    private final Map<String, Candidate> customLegacy = new ConcurrentHashMap<String, Candidate>();
    private final Map<String, String> customModern = new ConcurrentHashMap<String, String>();

    MaterialMappings() {}

    /**
     * Registers or replaces a mapping for a key.
     *
     * @param key Bloom key used in code/configs, e.g. {@code "SHOP_ICON"} or a modern material name
     * @param modernName material name on modern servers, or {@code null} if not available there
     * @param legacyName material name on legacy servers, or {@code null} if not available there
     * @param legacyData legacy data value
     * @return this instance
     */
    public MaterialMappings register(String key, String modernName, String legacyName, int legacyData) {
        String normalized = normalize(key);
        if (modernName != null) {
            customModern.put(normalized, normalize(modernName));
        }
        if (legacyName != null) {
            customLegacy.put(normalized, new Candidate(normalize(legacyName), (short) legacyData));
        }
        return this;
    }

    /**
     * Returns the names to try, in order, for a key on the given platform family.
     *
     * @param key material key
     * @param legacyPlatform whether the server uses legacy data values
     * @return ordered, de-duplicated candidates (never empty)
     */
    public List<Candidate> candidates(String key, boolean legacyPlatform) {
        String normalized = normalize(key);
        Set<Candidate> out = new LinkedHashSet<Candidate>();
        if (legacyPlatform) {
            Candidate custom = customLegacy.get(normalized);
            if (custom != null) {
                out.add(custom);
            }
            Candidate builtin = MODERN_TO_LEGACY.get(normalized);
            if (builtin != null) {
                out.add(builtin);
            }
            int colon = normalized.indexOf(':');
            if (colon > 0) {
                try {
                    short data = Short.parseShort(normalized.substring(colon + 1));
                    out.add(new Candidate(normalized.substring(0, colon), data));
                } catch (NumberFormatException e) {
                    out.add(new Candidate(normalized, (short) 0));
                }
            } else {
                out.add(new Candidate(normalized, (short) 0));
            }
        } else {
            String custom = customModern.get(normalized);
            if (custom != null) {
                out.add(new Candidate(custom, (short) 0));
            }
            if (normalized.indexOf(':') < 0) {
                out.add(new Candidate(normalized, (short) 0));
            }
            String builtin = LEGACY_TO_MODERN.get(normalized);
            if (builtin == null && normalized.endsWith(":0")) {
                builtin = LEGACY_TO_MODERN.get(normalized.substring(0, normalized.length() - 2));
            }
            if (builtin != null) {
                out.add(new Candidate(builtin, (short) 0));
            }
        }
        if (out.isEmpty()) {
            out.add(new Candidate(normalized, (short) 0));
        }
        return Collections.unmodifiableList(new ArrayList<Candidate>(out));
    }

    /**
     * Returns the number of built-in table rows (for diagnostics).
     *
     * @return number of built-in modern→legacy mappings
     */
    public static int builtinSize() {
        return MODERN_TO_LEGACY.size();
    }

    static String normalize(String key) {
        Objects.requireNonNull(key, "material key");
        String trimmed = key.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (trimmed.startsWith("MINECRAFT:")) {
            trimmed = trimmed.substring("MINECRAFT:".length());
        }
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Material key is empty");
        }
        return trimmed;
    }
}
