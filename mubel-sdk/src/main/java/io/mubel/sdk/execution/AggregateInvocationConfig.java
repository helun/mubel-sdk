package io.mubel.sdk.execution;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public record AggregateInvocationConfig<T, E, C>(
        Supplier<? extends T> aggregateSupplier,
        EventDispatcher<T, E> eventDispatcher,
        CommandDispatcher<T, E, C> commandDispatcher
) {
    public AggregateInvocationConfig {
        aggregateSupplier = requireNonNull(aggregateSupplier, "aggregateSupplier may not be null");
        eventDispatcher = requireNonNull(eventDispatcher, "eventDispatcher may not be null");
        commandDispatcher = requireNonNull(commandDispatcher, "commandDispatcher may not be null");
    }

    public static <T, E, C> AggregateInvocationConfig<T, E, C> of(
            Supplier<? extends T> aggregateSupplier,
            EventDispatcher<T, E> eventDispatcher,
            CommandDispatcher<T, E, C> commandDispatcher
    ) {
        return new AggregateInvocationConfig<>(aggregateSupplier, eventDispatcher, commandDispatcher);
    }

}
