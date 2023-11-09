package io.mubel.sdk.internal.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

record AggregateClassInfo(
        Class<?> aggregateClass,
        Constructor<?> constructor,
        Map<Class<?>, Optional<Method>> commandHandlers,
        Map<Class<?>, Optional<Method>> eventHandlers
) {
    static AggregateClassInfo create(
            Class<?> aggregateClass) {
        return new AggregateClassInfo(
                aggregateClass,
                AggregateClassUtil.findConstructor(aggregateClass)
                        .orElseThrow(() -> new NoSuchElementException("No public no-args constructor found for " + aggregateClass.getName())),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>()
        );
    }

    Optional<Method> findCommandHandler(Class<?> commandClass) {
        return commandHandlers.computeIfAbsent(
                commandClass,
                this::doFindCommandHandler
        );
    }

    private Optional<Method> doFindCommandHandler(Class<?> commandType) {
        Function<Class<?>, Predicate<Method>> predicateFn = aClass -> ClassUtil.PUBLIC_METHOD
                .and(ClassUtil.methodHasArgumentOfType(aClass))
                .and(ClassUtil.methodHasReturnType(List.class));

        return findHandler(
                commandType,
                predicateFn
        );
    }

    Optional<Method> findEventHandler(Class<?> eventClass) {
        return eventHandlers.computeIfAbsent(
                eventClass,
                this::doFindEventHandler
        );
    }

    private Optional<Method> doFindEventHandler(Class<?> eventType) {
        Function<Class<?>, Predicate<Method>> predicateFn = aClass -> ClassUtil.PUBLIC_METHOD
                .and(ClassUtil.methodHasArgumentOfType(aClass))
                .and(ClassUtil.methodHasVoidReturnType());

        return findHandler(
                eventType,
                predicateFn
        );
    }

    private Optional<Method> findHandler(Class<?> argumentType, Function<Class<?>, Predicate<Method>> predicateFn) {
        if (argumentType == null) {
            return Optional.empty();
        }
        return ClassUtil.findMethod(
                aggregateClass,
                predicateFn.apply(argumentType)
        ).or(() -> Stream.of(argumentType.getInterfaces())
                .filter(Objects::nonNull)
                .flatMap(c -> findHandler(c, predicateFn).stream())
                .findFirst()
        ).or(() -> findHandler(argumentType.getSuperclass(), predicateFn));
    }
}
