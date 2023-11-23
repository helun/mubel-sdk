package io.mubel.sdk.internal.reflection;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class CommandHandlerFinder implements Function<Class<?>, Predicate<Method>> {
    @Override
    public Predicate<Method> apply(Class<?> aClass) {
        return ClassUtil.PUBLIC_METHOD
                .and(ClassUtil.methodHasAnnotation(io.mubel.sdk.annotation.CommandHandler.class))
                .and(ClassUtil.methodHasArgumentOfType(aClass))
                .and(ClassUtil.methodHasReturnType(List.class)
                        .or(ClassUtil.hasEventLikeReturnType()));
    }
}
