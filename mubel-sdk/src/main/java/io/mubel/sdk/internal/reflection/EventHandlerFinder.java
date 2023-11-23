package io.mubel.sdk.internal.reflection;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;

public class EventHandlerFinder implements Function<Class<?>, Predicate<Method>> {
    @Override
    public Predicate<Method> apply(Class<?> aClass) {
        return ClassUtil.PUBLIC_METHOD
                .and(ClassUtil.methodHasAnnotation(io.mubel.sdk.annotation.EventHandler.class))
                .and(ClassUtil.methodHasArgumentOfType(aClass))
                .and(ClassUtil.methodHasVoidReturnType());
    }
}
