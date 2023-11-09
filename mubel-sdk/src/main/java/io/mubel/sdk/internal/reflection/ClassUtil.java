package io.mubel.sdk.internal.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

class ClassUtil {

    private static final Set<Class<?>> IRRELEVANT_RETURN_TYPES = Set.of(
            void.class,
            Void.class,
            Object.class,
            Boolean.class,
            boolean.class,
            int.class,
            Integer.class,
            String.class,
            Long.class,
            long.class,
            Double.class,
            double.class,
            Float.class,
            float.class,
            Short.class,
            short.class,
            Byte.class,
            byte.class,
            Character.class,
            char.class,
            Collection.class
    );

    static final Predicate<Method> PUBLIC_METHOD =
            method -> Modifier.isPublic(method.getModifiers());

    static final Predicate<Constructor<?>> PUBLIC_CONSTRUCTOR =
            constructor -> Modifier.isPublic(constructor.getModifiers());
    static final Predicate<Constructor<?>> NO_ARGS_CONSTRUCTOR =
            constructor -> constructor.getParameterCount() == 0;

    static final Predicate<Constructor<?>> PUBLIC_NO_ARGS_CONSTRUCTOR =
            PUBLIC_CONSTRUCTOR.and(NO_ARGS_CONSTRUCTOR);

    static Optional<Method> findMethod(Class<?> klass, Predicate<Method> predicate) {
        return Arrays.stream(klass.getMethods())
                .filter(predicate)
                .findFirst();
    }

    static Optional<Constructor<?>> findConstructor(Class<?> klass) {
        return Arrays.stream(klass.getConstructors())
                .filter(PUBLIC_NO_ARGS_CONSTRUCTOR)
                .findFirst();
    }

    static Predicate<Method> methodHasArgumentOfType(Class<?> klass) {
        return method -> Arrays.stream(method.getParameters())
                .anyMatch(param -> param.getType().equals(klass));
    }

    static Predicate<Method> methodHasReturnType(Class<?> klass) {
        return method -> method.getReturnType().isAssignableFrom(klass);
    }

    static Predicate<Method> methodHasVoidReturnType() {
        return method -> method.getReturnType() == void.class;
    }

    static Predicate<? super Method> hasEventLikeReturnType() {
        return method -> !IRRELEVANT_RETURN_TYPES.contains(method.getReturnType());
    }
}
