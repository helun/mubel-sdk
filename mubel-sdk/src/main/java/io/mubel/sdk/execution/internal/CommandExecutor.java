package io.mubel.sdk.execution.internal;

import io.mubel.sdk.HandlerResult;
import io.mubel.sdk.exceptions.MubelException;
import io.mubel.sdk.exceptions.MubelExecutionException;
import io.mubel.sdk.execution.AggregateInvocationConfig;
import io.mubel.sdk.execution.CommandDispatcher;
import io.mubel.sdk.execution.DeadlineDispatcher;
import io.mubel.sdk.execution.EventDispatcher;
import io.mubel.sdk.scheduled.ExpiredDeadline;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class CommandExecutor<T, E, C> {
    private final Supplier<? extends T> aggregateSupplier;
    private final EventDispatcher<T, E> eventDispatcher;
    private final CommandDispatcher<T, E, C> commandDispatcher;
    private final DeadlineDispatcher<T, E> deadlineDispatcher;

    public CommandExecutor(AggregateInvocationConfig<T, E, C> config) {
        final var nnConfig = requireNonNull(config, "config may not be null");
        this.aggregateSupplier = nnConfig.aggregateSupplier();
        this.eventDispatcher = nnConfig.eventDispatcher();
        this.commandDispatcher = nnConfig.commandDispatcher();
        this.deadlineDispatcher = nnConfig.deadlineDispatcher();
    }

    public HandlerResult<E> execute(List<E> existingEvents, C command) {
        try {
            final var aggregate = newAggregateInstance();
            applyEventsToAggregate(aggregate, existingEvents);
            final var handlerResult = executeCommand(command, aggregate);
            applyEventsToAggregate(aggregate, handlerResult.events());
            return handlerResult;
        } catch (MubelException me) {
            throw me;
        } catch (Exception e) {
            throw new MubelExecutionException(e);
        }
    }

    public HandlerResult<E> execute(Flux<E> existingEvents, C command) {
        try {
            final var aggregate = newAggregateInstance();
            applyEventsToAggregate(aggregate, existingEvents);
            final var handlerResult = executeCommand(command, aggregate);
            applyEventsToAggregate(aggregate, handlerResult.events());
            return handlerResult;
        } catch (MubelException me) {
            throw me;
        } catch (Exception e) {
            throw new MubelExecutionException(e);
        }
    }

    public HandlerResult<E> handleExpiredDeadline(Flux<E> existingEvents, ExpiredDeadline expiredDeadline) {
        try {
            final var aggregate = newAggregateInstance();
            applyEventsToAggregate(aggregate, existingEvents);
            final var handlerResult = executeDeadline(expiredDeadline, aggregate);
            applyEventsToAggregate(aggregate, handlerResult.events());
            return handlerResult;
        } catch (Exception e) {
            throw new MubelExecutionException(e);
        }
    }

    public Optional<T> getState(Flux<E> existingEvents) {
        try {
            final var aggregate = newAggregateInstance();
            final var eventCount = applyEventsToAggregate(aggregate, existingEvents);
            if (eventCount == 0) {
                return Optional.empty();
            }
            return Optional.of(aggregate);
        } catch (MubelException me) {
            throw me;
        } catch (Exception e) {
            throw new MubelExecutionException(e);
        }
    }

    private HandlerResult<E> executeCommand(C command, T aggregate) {
        final var handler = requireNonNull(commandDispatcher.resolveCommandHandler(aggregate));
        return requireNonNull(handler.apply(command), "resultingEvents may not be null");
    }

    private HandlerResult<E> executeDeadline(ExpiredDeadline deadline, T aggregate) {
        return requireNonNull(deadlineDispatcher.dispatch(aggregate, deadline), "resultingEvents may not be null");
    }

    private T newAggregateInstance() {
        return aggregateSupplier.get();
    }

    private void applyEventsToAggregate(T aggregate, List<? extends E> events) {
        if (events.isEmpty()) {
            return;
        }
        final var eventDispatcher = this.eventDispatcher.resolveEventHandler(aggregate);
        for (final var e : events) {
            eventDispatcher.accept(e);
        }
    }

    private int applyEventsToAggregate(T aggregate, Flux<? extends E> events) {
        final var eventDispatcher = this.eventDispatcher.resolveEventHandler(aggregate);
        var eventCounter = new AtomicInteger(0);
        for (var e : events.toIterable()) {
            eventDispatcher.accept(e);
            eventCounter.incrementAndGet();
        }
        return eventCounter.get();
    }
}
