package io.mubel.sdk.internal.reflection;

import io.mubel.sdk.scheduled.ExpiredDeadline;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.mubel.sdk.annotation.DeadlineHandler.DEFAULT_DEADLINE_NAME;

record AggregateClassInfo(
        Class<?> aggregateClass,
        Constructor<?> constructor,
        Map<Class<?>, Optional<Method>> commandHandlers,
        Map<Class<?>, Optional<Method>> eventHandlers,
        Map<String, Optional<Method>> deadlineHandlers
) {
    private final static CommandHandlerFinder COMMAND_HANDLER_FINDER = new CommandHandlerFinder();
    private final static EventHandlerFinder EVENT_HANDLER_FINDER = new EventHandlerFinder();

    static AggregateClassInfo create(
            Class<?> aggregateClass) {
        return new AggregateClassInfo(
                aggregateClass,
                AggregateClassUtil.findConstructor(aggregateClass)
                        .orElseThrow(() -> new NoSuchElementException("No public no-args constructor found for " + aggregateClass.getName())),
                new ConcurrentHashMap<>(),
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
        return findHandler(
                commandType,
                COMMAND_HANDLER_FINDER
        );
    }

    Optional<Method> findEventHandler(Class<?> eventClass) {
        return eventHandlers.computeIfAbsent(
                eventClass,
                this::doFindEventHandler
        );
    }


    Optional<? extends Method> findDeadlineHandler(ExpiredDeadline deadline) {
        return deadlineHandlers.computeIfAbsent(
                deadline.deadlineName(),
                this::doFindDeadlineHandler
        );
    }

    private Optional<Method> doFindDeadlineHandler(String deadlineName) {
        final var deadlineMethods = ClassUtil.findMethods(aggregateClass, DeadlineHandlerFinder.finder())
                .collect(Collectors.toMap(
                        DeadlineHandlerFinder::getDeadlineName,
                        Function.identity()
                ));
        return Optional.ofNullable(deadlineMethods.get(deadlineName))
                .or(() -> Optional.ofNullable(deadlineMethods.get(DEFAULT_DEADLINE_NAME)));
    }

    private Optional<Method> doFindEventHandler(Class<?> eventType) {
        return findHandler(
                eventType,
                EVENT_HANDLER_FINDER
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
