package org.hydra2s.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(
        mv = {1, 8, 0},
        k = 1,
        d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0002\b\u0002\bÆ\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\u001c\u0010\u0003\u001a\u00020\u00042\n\u0010\u0005\u001a\u0006\u0012\u0002\b\u00030\u00062\b\u0010\u0007\u001a\u0004\u0018\u00010\bJ\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u00012\b\u0010\f\u001a\u0004\u0018\u00010\bJ%\u0010\r\u001a\u0004\u0018\u0001H\u000e\"\u0004\b\u0000\u0010\u000e2\u0006\u0010\u000b\u001a\u00020\u00012\b\u0010\f\u001a\u0004\u0018\u00010\b¢\u0006\u0002\u0010\u000fJ\u000e\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u0004¨\u0006\u0013"},
        d2 = {"Lorg/hydra2s/manhack/mixin/util/ReflectionUtil;", "", "()V", "getField", "Ljava/lang/reflect/Field;", "clazz", "Ljava/lang/Class;", "fieldName", "", "getFieldFValue", "", "obj", "name", "getFieldValue", "T", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", "makeAccessible", "", "field", "manhack"}
)
@SourceDebugExtension({"SMAP\nReflectionUtil.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ReflectionUtil.kt\norg/hydra2s/manhack/mixin/util/ReflectionUtil\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,40:1\n1#2:41\n*E\n"})
public final class ReflectionUtil {
    @NotNull
    public static final ReflectionUtil INSTANCE = new ReflectionUtil();

    @NotNull
    public static final Field getField(@NotNull Class clazz, @Nullable String fieldName) throws Throwable {
        Intrinsics.checkNotNullParameter(clazz, "clazz");

        Field var10000;
        Field var3;
        try {
            var10000 = clazz.getDeclaredField(fieldName);
            Intrinsics.checkNotNullExpressionValue(var10000, "clazz.getDeclaredField(fieldName)");
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

    public static final void makeAccessible(@NotNull Field field) {
        Intrinsics.checkNotNullParameter(field, "field");
        if (Modifier.isPublic(field.getModifiers())) {
            Class var10000 = field.getDeclaringClass();
            Intrinsics.checkNotNullExpressionValue(var10000, "field.declaringClass");
            if (Modifier.isPublic(var10000.getModifiers())) {
                return;
            }
        }

        field.setAccessible(true);
    }

    public final float getFieldFValue(@NotNull Object obj, @Nullable String name) throws Throwable {
        Intrinsics.checkNotNullParameter(obj, "obj");
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

    @Nullable
    public static final Object getFieldValue(@NotNull Object obj, @Nullable String name) throws Throwable {
        Intrinsics.checkNotNullParameter(obj, "obj");
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
