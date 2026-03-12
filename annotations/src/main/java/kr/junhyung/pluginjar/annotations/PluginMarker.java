package kr.junhyung.pluginjar.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark the plugin main class.
 *
 * <p>Classes annotated with this are automatically detected at build time
 * and registered as the main class in {@code paper-plugin.yml} or {@code velocity-plugin.json}.</p>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface PluginMarker {
    String name() default "";
}