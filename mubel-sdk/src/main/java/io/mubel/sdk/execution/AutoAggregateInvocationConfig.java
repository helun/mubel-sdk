package io.mubel.sdk.execution;

import io.mubel.sdk.HandlerResult;
import io.mubel.sdk.exceptions.CommandHandlerException;
import io.mubel.sdk.exceptions.EventHandlerException;
import io.mubel.sdk.exceptions.HandlerNotFoundException;
import io.mubel.sdk.internal.reflection.AggregateClassUtil;
import io.mubel.sdk.scheduled.ExpiredDeadline;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AutoAggregateInvocationConfig {

    public static <T> AggregateInvocationConfig<T, Object, Object> of(Class<T> aggregateClass) {
        return of(aggregateClass, Object.class, Object.class);
    }

    @SuppressWarnings("unchecked")
    public static <T, E, C> AggregateInvocationConfig<T, E, C> of(
            Class<T> aggregateClass,
            Class<E> eventBaseClass,
            Class<C> commandBaseClass
    ) {
        final Supplier<T> aggregateSupplier = () -> (T) AggregateClassUtil.newInstance(aggregateClass);
        final EventDispatcher<T, E> eventDispatcher = a -> reflectionEventDispatcher(aggregateClass, a);
        final CommandDispatcher<T, E, C> commandDispatcher = a -> reflectionCommandDispatcher(aggregateClass, a);
        final DeadlineDispatcher<T, E> deadlineHandler = reflectionDeadlineDispatcher(aggregateClass);

        return new AggregateInvocationConfig<>(
                aggregateClass.getSimpleName(),
                aggregateSupplier,
                eventDispatcher,
                commandDispatcher,
                deadlineHandler
        );
    }

    @SuppressWarnings("unchecked")
    public static <E, C, T> Function<C, HandlerResult<E>> reflectionCommandDispatcher(Class<T> aggregateClass, T aggregateInstance) {
        return (C command) ->
                AggregateClassUtil.findCommandHandler(aggregateClass, command.getClass())
                        .map(handler -> (HandlerResult<E>) invokeCommandHandler(aggregateInstance, command, handler))
                        .orElseThrow(() -> HandlerNotFoundException.forCommand("No command handler found for " + command.getClass()));
    }

    @SuppressWarnings("unchecked")
    private static <T, E> DeadlineDispatcher<T, E> reflectionDeadlineDispatcher(Class<T> aggregateClass) {
        return (aggregateInstance, deadline) ->
                AggregateClassUtil.findDeadlineHandler(aggregateClass, deadline)
                        .map(handler -> (HandlerResult<E>) invokeDeadlineHandler(aggregateInstance, handler, deadline))
                        .orElseThrow(() -> HandlerNotFoundException.forDeadline("No deadline handler found for " + deadline.deadlineName()));
    }

    public static <T, E> Consumer<E> reflectionEventDispatcher(Class<T> aggregateClass, T aggregateInstance) {
        return (E event) ->
                AggregateClassUtil.findEventHandler(aggregateClass, event.getClass())
                        .ifPresentOrElse(
                                m -> invokeEventHandler(aggregateInstance, event, m),
                                () -> {
                                    throw HandlerNotFoundException.forEvent("No event handler found for " + event.getClass());
                                }
                        );
    }

    private static <T, E> Object invokeEventHandler(T aggregateInstance, E event, Method eventHandler) {
        try {
            return eventHandler.invoke(aggregateInstance, event);
        } catch (Exception e) {
            throw new EventHandlerException(formatHandlerErrorMessage(aggregateInstance, eventHandler), e);
        }
    }


    private static <T, C, E> HandlerResult<E> invokeCommandHandler(T aggregateInstance, C command, Method commandHandler) {
        try {
            final var result = commandHandler.invoke(aggregateInstance, command);
            return toHandlerResult(result);
        } catch (Exception e) {
            throw new CommandHandlerException(formatHandlerErrorMessage(aggregateInstance, commandHandler), e);
        }
    }

    private static <T, E> HandlerResult<E> invokeDeadlineHandler(T aggregateInstance, Method deadlineHandler, ExpiredDeadline deadline) {
        try {
            final Object result;
            if (deadlineHandler.getParameterCount() == 1) {
                result = deadlineHandler.invoke(aggregateInstance, deadline);
            } else {
                result = deadlineHandler.invoke(aggregateInstance);
            }
            return toHandlerResult(result);
        } catch (Exception e) {
            throw new CommandHandlerException(formatHandlerErrorMessage(aggregateInstance, deadlineHandler), e);
        }
    }

    private static <T> String formatHandlerErrorMessage(T a, Method handler) {
        return "Caught exception while invoking %s.%s".formatted(a.getClass().getName(), handler.getName());
    }

    @SuppressWarnings("unchecked")
    private static <E> HandlerResult<E> toHandlerResult(Object result) {
        if (result instanceof List<?> list) {
            return HandlerResult.of((List<E>) result).build();
        } else if (result instanceof HandlerResult<?> hr) {
            return (HandlerResult<E>) hr;
        } else {
            return HandlerResult.of(List.of((E) result)).build();
        }
    }

}
