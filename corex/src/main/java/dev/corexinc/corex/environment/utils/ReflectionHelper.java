package dev.corexinc.corex.environment.utils;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

@SuppressWarnings("deprecation")
public class ReflectionHelper {

    private static Unsafe unsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setFinalField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            long offset = unsafe.objectFieldOffset(field);
            unsafe.putObject(target, offset, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setFinalFieldByType(Object target, Class<?> fieldType, Object value) {
        try {
            for (Field field : target.getClass().getDeclaredFields()) {
                if (field.getType().equals(fieldType)) {
                    long offset = unsafe.objectFieldOffset(field);
                    unsafe.putObject(target, offset, value);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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