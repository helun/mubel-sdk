package io.mubel.sdk.internal.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

class ClassUtil {

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
    
}
