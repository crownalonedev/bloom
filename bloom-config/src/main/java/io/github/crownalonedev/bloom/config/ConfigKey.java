package io.github.crownalonedev.bloom.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the YAML key of a mapped field. Without it, field names are converted from camelCase to kebab-case
 * ({@code maxHomes} → {@code max-homes}).
 *
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigKey {

    /**
     * Returns the YAML key.
     *
     * @return key
     */
    String value();
}
