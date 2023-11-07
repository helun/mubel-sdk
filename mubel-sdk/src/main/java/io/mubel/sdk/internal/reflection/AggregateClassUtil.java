package io.mubel.sdk.internal.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public class AggregateClassUtil {

    private static final ClassValue<AggregateClassInfo> AGGREGATE_INFOS = new ClassValue<>() {
        @Override
        protected AggregateClassInfo computeValue(Class<?> type) {
            return AggregateClassInfo.create(type);
        }
    };

    public static final Predicate<Constructor<?>> PUBLIC_NO_ARGS_CONSTRUCTOR =
            ClassUtil.PUBLIC_CONSTRUCTOR.and(ClassUtil.NO_ARGS_CONSTRUCTOR);

    public static Object newInstance(Class<?> aggregateClass) {
        var info = AGGREGATE_INFOS.get(aggregateClass);
        try {
            return info.constructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<Method> findCommandHandler(Class<?> aggregateClass, Class<?> commandClass) {
        return Optional.of(AGGREGATE_INFOS.get(aggregateClass))
                .flatMap(info -> info.findCommandHandler(commandClass));
    }

    public static Optional<Method> findEventHandler(Class<?> aggregateClass, Class<?> eventClass) {
        return Optional.of(AGGREGATE_INFOS.get(aggregateClass))
                .flatMap(info -> info.findEventHandler(eventClass));
    }

    public static Optional<Constructor<?>> findConstructor(Class<?> klass) {
        return Arrays.stream(klass.getConstructors())
                .filter(PUBLIC_NO_ARGS_CONSTRUCTOR)
                .findFirst()
                .or(() -> getDefailtConstructor(klass));
    }

    private static Optional<Constructor<?>> getDefailtConstructor(Class<?> klass) {
        try {
            return Optional.of(klass.getConstructor());
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }
}
