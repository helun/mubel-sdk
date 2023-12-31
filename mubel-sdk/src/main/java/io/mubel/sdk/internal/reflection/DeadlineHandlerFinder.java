package io.mubel.sdk.internal.reflection;

import io.mubel.sdk.annotation.DeadlineHandler;
import io.mubel.sdk.scheduled.ExpiredDeadline;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

public final class DeadlineHandlerFinder {

    private DeadlineHandlerFinder() {
    }

    public static Predicate<Method> finder() {
        return ClassUtil.PUBLIC_METHOD
                .and(ClassUtil.methodHasAnnotation(DeadlineHandler.class))
                .and(ClassUtil.hasNoArguments()
                        .or(ClassUtil.methodHasArgumentOfType(ExpiredDeadline.class))
                ).and(ClassUtil.methodHasReturnType(List.class)
                        .or(ClassUtil.hasEventLikeReturnType()));
    }

    public static String getDeadlineName(Method m) {
        return m.getAnnotation(DeadlineHandler.class).value();
    }

}
