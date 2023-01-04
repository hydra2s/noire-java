package org.hydra2s.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class ReflectionUtil {
    public static final ReflectionUtil INSTANCE = new ReflectionUtil();
    public static final Field getField(Class clazz, String fieldName) throws Throwable {
        Field var10000;
        Field var3;
        try {
            var10000 = clazz.getDeclaredField(fieldName);
            var3 = var10000;
        } catch (NoSuchFieldException var9) {
            Class superClass = clazz.getSuperclass();
            if (superClass != null) {
                boolean var8 = false;
                var10000 = INSTANCE.getField(superClass, fieldName);
                if (var10000 != null) {
                    var3 = var10000;
                    return var3;
                }
            }

            throw (Throwable)var9;
        }

        return var3;
    }

    public static final void makeAccessible(Field field) {
        if (Modifier.isPublic(field.getModifiers())) {
            Class var10000 = field.getDeclaringClass();
            if (Modifier.isPublic(var10000.getModifiers())) {
                return;
            }
        }

        field.setAccessible(true);
    }

    public final float getFieldFValue(Object obj, String name) throws Throwable {
        Class var10000 = obj.getClass();
        Field var7;
        if (var10000 != null) {
            Class var4 = var10000;
            boolean var6 = false;
            var7 = INSTANCE.getField(var4, name);
        } else {
            var7 = null;
        }

        Field field = var7;
        if (field != null) {
            makeAccessible(field);
        }

        return field != null ? field.getFloat(obj) : 0.0F;
    }

    public static final Object getFieldValue(Object obj, String name) throws Throwable {
        Class var10000 = obj.getClass();
        Field var7;
        if (var10000 != null) {
            Class var4 = var10000;
            boolean var6 = false;
            var7 = INSTANCE.getField(var4, name);
        } else {
            var7 = null;
        }

        Field field = var7;
        if (field != null) {
            makeAccessible(field);
        }

        return field != null ? field.get(obj) : null;
    }

    private ReflectionUtil() {
    }
}
