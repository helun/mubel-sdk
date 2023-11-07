package io.mubel.sdk.execution.internal;

import io.mubel.sdk.exceptions.MubelException;
import io.mubel.sdk.exceptions.MubelExecutionException;
import io.mubel.sdk.execution.AggregateInvocationConfig;
import io.mubel.sdk.execution.CommandDispatcher;
import io.mubel.sdk.execution.EventDispatcher;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class CommandExecutor<T, E, C> {
    private final Supplier<? extends T> aggregateSupplier;
    private final EventDispatcher<T, E> eventDispatcher;
    private final CommandDispatcher<T, E, C> commandDispatcher;

    public CommandExecutor(AggregateInvocationConfig<T, E, C> config) {
        final var nnConfig = requireNonNull(config, "config may not be null");
        this.aggregateSupplier = nnConfig.aggregateSupplier();
        this.eventDispatcher = nnConfig.eventDispatcher();
        this.commandDispatcher = nnConfig.commandDispatcher();
    }

    public List<E> execute(List<E> existingEvents, C command) {
        try {
            final var aggregate = newAggregateInstance();
            applyEventsToAggregate(aggregate, existingEvents);
            final var resultingEvents = executeCommand(command, aggregate);
            applyEventsToAggregate(aggregate, resultingEvents);
            return resultingEvents;
        } catch (MubelException me) {
            throw me;
        } catch (Exception e) {
            throw new MubelExecutionException(e);
        }
    }

    public T getState(List<E> existingEvents) {
        try {
            final var aggregate = newAggregateInstance();
            applyEventsToAggregate(aggregate, existingEvents);
            return aggregate;
        } catch (MubelException me) {
            throw me;
        } catch (Exception e) {
            throw new MubelExecutionException(e);
        }
    }

    private List<E> executeCommand(C command, T aggregate) {
        final var handler = requireNonNull(commandDispatcher.resolveCommandHandler(aggregate), "");
        return requireNonNull(handler.apply(command), "resultingEvents may not be null");
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

}
