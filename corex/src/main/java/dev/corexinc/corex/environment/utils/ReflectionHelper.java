package dev.corexinc.corex.environment.utils;

import dev.corexinc.corex.engine.utils.CorexLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Patches fields on NMS objects that Mojang declared final.
 *
 * <p>This used to go through {@code sun.misc.Unsafe}, whose memory-access methods are
 * deprecated for removal - once they go, so would biome patching, and until then Java 24
 * and up print a warning on startup just for touching them.
 *
 * <p>Plain reflection replaces it. Java only refuses {@code Field.set} on a final field
 * when the field is static, or when its class is a record or a hidden class; an ordinary
 * instance field yields to {@code setAccessible}. Everything patched here is an instance
 * field on a regular class, so nothing is lost. {@code VarHandle} is not an option: it
 * throws {@code UnsupportedOperationException} on any write to a final field.
 */
public class ReflectionHelper {

    /**
     * Assigns a final instance field by name.
     */
    public static void setFinalField(Object target, String fieldName, Object value) {
        try {
            assign(target, target.getClass().getDeclaredField(fieldName), value);
        } catch (NoSuchFieldException e) {
            CorexLogger.error("No field '" + fieldName + "' on " + target.getClass().getSimpleName() + ".");
        } catch (Exception e) {
            reportFailure(target, "'" + fieldName + "'", e);
        }
    }

    /**
     * Assigns the first final instance field of the given type.
     * <p>
     * Used where a mapping-dependent field name would break between versions but the type
     * is unique within the class.
     */
    public static void setFinalFieldByType(Object target, Class<?> fieldType, Object value) {
        try {
            for (Field field : target.getClass().getDeclaredFields()) {
                if (field.getType().equals(fieldType) && !Modifier.isStatic(field.getModifiers())) {
                    assign(target, field, value);
                    return;
                }
            }
            CorexLogger.error("No field of type " + fieldType.getSimpleName()
                    + " on " + target.getClass().getSimpleName() + ".");
        } catch (Exception e) {
            reportFailure(target, "of type " + fieldType.getSimpleName(), e);
        }
    }

    private static void assign(Object target, Field field, Object value) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void reportFailure(Object target, String what, Exception cause) {
        String owner = target.getClass().getSimpleName();
        CorexLogger.error("Failed to set field " + what + " on " + owner + ": " + cause.getMessage());

        if (target.getClass().isRecord()) {
            CorexLogger.error("  <gray>" + owner + " is a record, and Java forbids writing record components. "
                    + "This version needs the object rebuilt rather than patched.");
        }
    }

    public static Object getFieldValue(Class<?> clazz, String fieldName, Object instance) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception e) {
            return null;
        }
    }
}
