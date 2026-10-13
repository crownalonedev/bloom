package io.github.crownalonedev.bloom.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Collects validation rules and reports every problem at once. Created by {@link YamlConfig#validate()}.
 *
 * <pre>{@code
 * config.validate()
 *         .require("database.type")
 *         .oneOf("database.type", "sqlite", "mysql", "mariadb")
 *         .range("limits.homes", 1, 100)
 *         .type("cooldowns.pearl", java.time.Duration.class)
 *         .check();   // throws ConfigException listing all problems with line numbers
 * }</pre>
 *
 * @since 0.1.0
 */
public final class ConfigValidation {

    private final YamlConfig config;
    private final List<String> problems = new ArrayList<String>();

    ConfigValidation(YamlConfig config) {
        this.config = config;
    }

    /**
     * Requires a value to exist (in the file or defaults).
     *
     * @param path path
     * @return this validation
     */
    public ConfigValidation require(String path) {
        if (!config.has(path)) {
            problems.add(config.locate(path) + " is missing");
        }
        return this;
    }

    /**
     * Requires a present value to convert to a type.
     *
     * @param path path
     * @param type expected type
     * @return this validation
     */
    public ConfigValidation type(String path, Class<?> type) {
        if (config.has(path)) {
            try {
                config.get(path, type);
            } catch (ConfigException e) {
                problems.add(e.getMessage());
            }
        }
        return this;
    }

    /**
     * Requires a present numeric value to lie in an inclusive range.
     *
     * @param path path
     * @param min minimum
     * @param max maximum
     * @return this validation
     */
    public ConfigValidation range(String path, double min, double max) {
        if (config.has(path)) {
            try {
                double value = config.getDouble(path);
                if (value < min || value > max) {
                    problems.add(config.locate(path) + " must be between " + trim(min) + " and " + trim(max)
                            + ", but is " + trim(value));
                }
            } catch (ConfigException e) {
                problems.add(e.getMessage());
            }
        }
        return this;
    }

    /**
     * Requires a present text value to be one of the allowed values (case-insensitive).
     *
     * @param path path
     * @param allowed allowed values
     * @return this validation
     */
    public ConfigValidation oneOf(String path, String... allowed) {
        if (config.has(path)) {
            String value = config.getString(path);
            for (String option : allowed) {
                if (option.equalsIgnoreCase(value)) {
                    return this;
                }
            }
            problems.add(config.locate(path) + " must be one of "
                    + Arrays.toString(allowed).toLowerCase(Locale.ROOT) + ", but is '" + value + "'");
        }
        return this;
    }

    /**
     * Requires a present text value to match a regular expression.
     *
     * @param path path
     * @param pattern pattern the whole value must match
     * @param description human description of the expected format
     * @return this validation
     */
    public ConfigValidation matches(String path, Pattern pattern, String description) {
        if (config.has(path)) {
            String value = config.getString(path);
            if (!pattern.matcher(value).matches()) {
                problems.add(config.locate(path) + " must be " + description + ", but is '" + value + "'");
            }
        }
        return this;
    }

    /**
     * Adds a custom rule on a present value.
     *
     * @param path path
     * @param type type to convert the value to
     * @param rule condition that must hold
     * @param message problem description if the rule fails, e.g. {@code "must be a power of two"}
     * @param <T> value type
     * @return this validation
     */
    public <T> ConfigValidation check(String path, Class<T> type, Predicate<T> rule, String message) {
        if (config.has(path)) {
            try {
                if (!rule.test(config.get(path, type))) {
                    problems.add(config.locate(path) + " " + message);
                }
            } catch (ConfigException e) {
                problems.add(e.getMessage());
            }
        }
        return this;
    }

    /**
     * Returns the problems found so far.
     *
     * @return problem descriptions
     */
    public List<String> problems() {
        return new ArrayList<String>(problems);
    }

    /**
     * Throws if any rule failed.
     *
     * @throws ConfigException listing every problem
     */
    public void check() {
        if (!problems.isEmpty()) {
            throw new ConfigException(
                    config.file().getFileName() + " has " + problems.size() + " problem"
                            + (problems.size() == 1 ? "" : "s") + ":",
                    problems);
        }
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
