package io.mubel.sdk.execution;

import io.mubel.sdk.exceptions.CommandHandlerException;
import io.mubel.sdk.exceptions.EventHandlerException;
import io.mubel.sdk.exceptions.NoCommandHandlerFoundException;
import io.mubel.sdk.internal.reflection.AggregateClassUtil;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AutoAggregateInvocationConfig {

    public static <T> AggregateInvocationConfig<T, Object, Object> of(Class<T> aggregateClass) {
        return of(aggregateClass, Object.class, Object.class);
    }

    public static <T, E, C> AggregateInvocationConfig<T, E, C> of(
            Class<T> aggregateClass,
            Class<E> eventBaseClass,
            Class<C> commandBaseClass
    ) {
        final Supplier<T> aggregateSupplier = () -> (T) AggregateClassUtil.newInstance(aggregateClass);
        final EventDispatcher<T, E> eventDispatcher = a -> reflectionEventDispatcher(aggregateClass, a);
        final CommandDispatcher<T, E, C> commandDispatcher = a -> reflectionCommandDispatcher(aggregateClass, a);
        return new AggregateInvocationConfig<>(aggregateSupplier, eventDispatcher, commandDispatcher);
    }

    private static <E, C, T> Function<C, List<E>> reflectionCommandDispatcher(Class<T> aggregateClass, T aggregateInstance) {
        return (C command) ->
                AggregateClassUtil.findCommandHandler(aggregateClass, command.getClass())
                        .map(handler -> (List<E>) invokeCommandHandler(aggregateInstance, command, handler))
                        .orElseThrow(() -> new NoCommandHandlerFoundException("No command handler found for " + command.getClass()));
    }

    private static <T, E> Consumer<E> reflectionEventDispatcher(Class<T> aggregateClass, T aggregateInstance) {
        return (E event) ->
                AggregateClassUtil.findEventHandler(aggregateClass, event.getClass())
                        .ifPresent(m -> invokeEventHandler(aggregateInstance, event, m));
    }

    private static <T, E> Object invokeEventHandler(T a, E event, Method eventHandler) {
        try {
            return eventHandler.invoke(a, event);
        } catch (Exception e) {
            throw new EventHandlerException("Caught exception while invoking %s".formatted(eventHandler.getName()), e);
        }
    }

    private static <T, C, E> List<E> invokeCommandHandler(T aggregateInstance, C command, Method commandHandler) {
        try {
            return (List<E>) commandHandler.invoke(aggregateInstance, command);
        } catch (Exception e) {
            throw new CommandHandlerException("Caught exception while invoking %s".formatted(commandHandler.getName()), e);
        }
    }

}
