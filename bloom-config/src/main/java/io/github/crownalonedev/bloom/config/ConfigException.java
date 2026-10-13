package io.github.crownalonedev.bloom.config;

import io.github.crownalonedev.bloom.api.BloomException;
import java.util.Collections;
import java.util.List;

/**
 * Thrown for configuration problems: unreadable YAML, missing or mistyped values, failed validation or migration.
 * Messages name the file, the key path and, when known, the line number.
 *
 * @since 0.1.0
 */
public class ConfigException extends BloomException {

    private static final long serialVersionUID = 1L;

    private final List<String> problems;

    /**
     * Creates an exception with one problem.
     *
     * @param message description including file/path/line
     */
    public ConfigException(String message) {
        super(message);
        this.problems = Collections.singletonList(message);
    }

    /**
     * Creates an exception with a cause.
     *
     * @param message description
     * @param cause cause
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
        this.problems = Collections.singletonList(message);
    }

    /**
     * Creates an exception listing several problems (validation).
     *
     * @param summary first line of the message
     * @param problems individual problems
     */
    public ConfigException(String summary, List<String> problems) {
        super(summary + format(problems));
        this.problems = Collections.unmodifiableList(problems);
    }

    /**
     * Returns the individual problems.
     *
     * @return problems, at least one
     */
    public List<String> problems() {
        return problems;
    }

    private static String format(List<String> problems) {
        StringBuilder out = new StringBuilder();
        for (String problem : problems) {
            out.append("\n  - ").append(problem);
        }
        return out.toString();
    }
}
