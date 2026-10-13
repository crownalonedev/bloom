package io.github.crownalonedev.bloom.config.internal;

import io.github.crownalonedev.bloom.api.util.Durations;
import io.github.crownalonedev.bloom.config.ConfigKey;
import io.github.crownalonedev.bloom.config.TypeSerializer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts between YAML values and Java types: scalars, enums, UUID, Duration, List/Set/Map and simple POJOs.
 * Internal; failures throw {@link IllegalArgumentException} with a short reason that callers enrich.
 */
public final class ObjectMapper {

    private final Map<Class<?>, TypeSerializer<?>> serializers = new ConcurrentHashMap<Class<?>, TypeSerializer<?>>();

    /**
     * Registers a serializer.
     *
     * @param type handled type
     * @param serializer serializer
     * @param <T> type
     */
    public <T> void register(Class<T> type, TypeSerializer<T> serializer) {
        serializers.put(type, serializer);
    }

    /**
     * Converts a YAML value to a Java type.
     *
     * @param raw YAML value, not {@code null}
     * @param type target type (may be parameterized)
     * @return converted value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object deserialize(Object raw, Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;
            Class<?> rawClass = (Class<?>) parameterized.getRawType();
            Type[] args = parameterized.getActualTypeArguments();
            if (List.class.isAssignableFrom(rawClass)
                    || Set.class.isAssignableFrom(rawClass)
                    || rawClass == Collection.class) {
                Collection<Object> out =
                        Set.class.isAssignableFrom(rawClass) ? new LinkedHashSet<Object>() : new ArrayList<Object>();
                for (Object element : asList(raw)) {
                    out.add(element == null ? null : deserialize(element, args[0]));
                }
                return out;
            }
            if (Map.class.isAssignableFrom(rawClass)) {
                if (args[0] != String.class) {
                    throw new IllegalArgumentException("only Map<String, ...> is supported");
                }
                Map<String, Object> out = new LinkedHashMap<String, Object>();
                for (Map.Entry<String, Object> entry : asMap(raw).entrySet()) {
                    out.put(entry.getKey(), entry.getValue() == null ? null : deserialize(entry.getValue(), args[1]));
                }
                return out;
            }
            type = rawClass;
        }
        Class<?> target = (Class<?>) type;
        TypeSerializer<?> custom = serializers.get(target);
        if (custom != null) {
            return custom.deserialize(raw);
        }
        if (target == Object.class) {
            return raw;
        }
        if (target == String.class) {
            if (raw instanceof Map || raw instanceof List) {
                throw new IllegalArgumentException("expected text but found a " + describe(raw));
            }
            return String.valueOf(raw);
        }
        if (target == int.class || target == Integer.class) {
            return (int) integral(raw, Integer.MIN_VALUE, Integer.MAX_VALUE, "a whole number");
        }
        if (target == long.class || target == Long.class) {
            return integral(raw, Long.MIN_VALUE, Long.MAX_VALUE, "a whole number");
        }
        if (target == short.class || target == Short.class) {
            return (short) integral(raw, Short.MIN_VALUE, Short.MAX_VALUE, "a whole number");
        }
        if (target == byte.class || target == Byte.class) {
            return (byte) integral(raw, Byte.MIN_VALUE, Byte.MAX_VALUE, "a whole number");
        }
        if (target == double.class || target == Double.class) {
            return decimal(raw);
        }
        if (target == float.class || target == Float.class) {
            return (float) decimal(raw);
        }
        if (target == boolean.class || target == Boolean.class) {
            return bool(raw);
        }
        if (target == char.class || target == Character.class) {
            String text = String.valueOf(raw);
            if (text.length() != 1) {
                throw new IllegalArgumentException("expected a single character but found '" + text + "'");
            }
            return text.charAt(0);
        }
        if (target.isEnum()) {
            return enumValue((Class<? extends Enum>) target, String.valueOf(raw));
        }
        if (target == UUID.class) {
            try {
                return UUID.fromString(String.valueOf(raw));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("expected a UUID but found '" + raw + "'");
            }
        }
        if (target == Duration.class) {
            if (raw instanceof Number) {
                return Duration.ofSeconds(((Number) raw).longValue());
            }
            return Durations.parse(String.valueOf(raw));
        }
        if (List.class.isAssignableFrom(target)) {
            return new ArrayList<Object>(asList(raw));
        }
        if (Map.class.isAssignableFrom(target)) {
            return new LinkedHashMap<String, Object>(asMap(raw));
        }
        return pojo(target, asMap(raw));
    }

    /**
     * Converts a Java value to a plain YAML value.
     *
     * @param value value, may be {@code null}
     * @return YAML value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object serialize(Object value) {
        if (value == null) {
            return null;
        }
        TypeSerializer custom = serializers.get(value.getClass());
        if (custom != null) {
            return custom.serialize(value);
        }
        if (value instanceof String
                || value instanceof Boolean
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Double
                || value instanceof Float
                || value instanceof Short
                || value instanceof Byte) {
            return value;
        }
        if (value instanceof Character || value instanceof UUID) {
            return value.toString();
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof Duration) {
            Duration duration = (Duration) value;
            if (duration.toMillis() % 1000 != 0) {
                return duration.toMillis() + "ms";
            }
            return Durations.format(duration).replace(" ", "");
        }
        if (value instanceof Collection) {
            List<Object> out = new ArrayList<Object>();
            for (Object element : (Collection<Object>) value) {
                out.add(serialize(element));
            }
            return out;
        }
        if (value instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                out.put(String.valueOf(entry.getKey()), serialize(entry.getValue()));
            }
            return out;
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (Field field : fields(value.getClass())) {
            try {
                Object fieldValue = field.get(value);
                if (fieldValue != null) {
                    out.put(keyOf(field), serialize(fieldValue));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot read field " + field, e);
            }
        }
        return out;
    }

    private Object pojo(Class<?> type, Map<String, Object> values) {
        Object instance;
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(type.getSimpleName() + " needs a no-argument constructor to be mapped "
                    + "from config (or register a TypeSerializer for it)");
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("cannot create " + type.getSimpleName() + ": " + e.getMessage());
        }
        for (Field field : fields(type)) {
            String key = keyOf(field);
            Object raw = values.get(key);
            if (raw == null) {
                continue; // keep the field's default value
            }
            try {
                field.set(instance, deserialize(raw, field.getGenericType()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("'" + key + "': " + e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot write field " + field, e);
            }
        }
        return instance;
    }

    private static List<Field> fields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields;
    }

    /**
     * Returns the YAML key of a field.
     *
     * @param field field
     * @return key
     */
    static String keyOf(Field field) {
        ConfigKey key = field.getAnnotation(ConfigKey.class);
        return key != null ? key.value() : kebab(field.getName());
    }

    /**
     * Converts camelCase to kebab-case.
     *
     * @param name camelCase name
     * @return kebab-case name
     */
    public static String kebab(String name) {
        StringBuilder out = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    out.append('-');
                }
                out.append(Character.toLowerCase(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static long integral(Object raw, long min, long max, String expected) {
        long value;
        if (raw instanceof Integer || raw instanceof Long || raw instanceof Short || raw instanceof Byte) {
            value = ((Number) raw).longValue();
        } else if (raw instanceof java.math.BigInteger) {
            throw new IllegalArgumentException("number " + raw + " is too large");
        } else if (raw instanceof Number) {
            double d = ((Number) raw).doubleValue();
            if (d != Math.rint(d)) {
                throw new IllegalArgumentException("expected " + expected + " but found " + raw);
            }
            value = (long) d;
        } else if (raw instanceof String) {
            try {
                value = Long.parseLong(((String) raw).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("expected " + expected + " but found '" + raw + "'");
            }
        } else {
            throw new IllegalArgumentException("expected " + expected + " but found a " + describe(raw));
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException("number " + value + " is out of range (" + min + " to " + max + ")");
        }
        return value;
    }

    private static double decimal(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        if (raw instanceof String) {
            try {
                return Double.parseDouble(((String) raw).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("expected a number but found '" + raw + "'");
            }
        }
        throw new IllegalArgumentException("expected a number but found a " + describe(raw));
    }

    private static boolean bool(Object raw) {
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        String text = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        if (text.equals("true") || text.equals("yes") || text.equals("on")) {
            return true;
        }
        if (text.equals("false") || text.equals("no") || text.equals("off")) {
            return false;
        }
        throw new IllegalArgumentException("expected true or false but found '" + raw + "'");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<? extends Enum> type, String text) {
        String normalized =
                text.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (Enum constant : type.getEnumConstants()) {
            if (constant.name().equals(normalized)) {
                return constant;
            }
        }
        List<String> names = new ArrayList<String>();
        for (Enum constant : type.getEnumConstants()) {
            names.add(constant.name().toLowerCase(Locale.ROOT));
        }
        throw new IllegalArgumentException("'" + text + "' is not one of " + names);
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object raw) {
        if (raw instanceof List) {
            return (List<Object>) raw;
        }
        throw new IllegalArgumentException("expected a list but found a " + describe(raw));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object raw) {
        if (raw instanceof Map) {
            return (Map<String, Object>) raw;
        }
        throw new IllegalArgumentException("expected a section (key: value pairs) but found a " + describe(raw));
    }

    static String describe(Object raw) {
        if (raw instanceof Map) {
            return "section";
        }
        if (raw instanceof List) {
            return "list";
        }
        if (raw instanceof String) {
            return "text value '" + raw + "'";
        }
        return raw.getClass().getSimpleName().toLowerCase(Locale.ROOT) + " value " + raw;
    }
}
