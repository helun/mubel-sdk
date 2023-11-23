package io.mubel.sdk.execution;

import io.mubel.sdk.internal.Utils;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

public record AggregateInvocationConfig<T, E, C>(
        String aggregateName,
        Supplier<? extends T> aggregateSupplier,
        EventDispatcher<T, E> eventDispatcher,
        CommandDispatcher<T, E, C> commandDispatcher,
        DeadlineDispatcher<T, E> deadlineDispatcher
) {
    public AggregateInvocationConfig {
        aggregateName = requireNonNull(aggregateName, "aggregateName may not be null");
        aggregateSupplier = requireNonNull(aggregateSupplier, "aggregateSupplier may not be null");
        eventDispatcher = requireNonNull(eventDispatcher, "eventDispatcher may not be null");
        commandDispatcher = requireNonNull(commandDispatcher, "commandDispatcher may not be null");
        deadlineDispatcher = requireNonNull(deadlineDispatcher, "deadlineDispatcher may not be null");
    }

    public static <T, E, C> Builder<T, E, C> builder(
            Class<T> aggregateClass,
            Class<E> eventBaseClass,
            Class<C> commandBaseClass
    ) {
        return new Builder<>(aggregateClass, eventBaseClass, commandBaseClass);
    }

    public static class Builder<T, E, C> {

        private final Class<T> aggregateClass;
        private final Class<E> eventBaseClass;
        private final Class<C> commandBaseClass;
        private String aggregateName;
        private Supplier<? extends T> aggregateSupplier;
        private EventDispatcher<T, E> eventDispatcher;
        private CommandDispatcher<T, E, C> commandDispatcher;
        private DeadlineDispatcher<T, E> deadlineDispatcher;

        public Builder(Class<T> aggregateClass, Class<E> eventBaseClass, Class<C> commandBaseClass) {
            this.aggregateClass = aggregateClass;
            this.eventBaseClass = eventBaseClass;
            this.commandBaseClass = commandBaseClass;
        }

        public Builder<T, E, C> aggregateName(String aggregateName) {
            this.aggregateName = aggregateName;
            return this;
        }

        public Builder<T, E, C> aggregateSupplier(Supplier<? extends T> aggregateSupplier) {
            this.aggregateSupplier = aggregateSupplier;
            return this;
        }

        public Builder<T, E, C> eventDispatcher(EventDispatcher<T, E> eventDispatcher) {
            this.eventDispatcher = eventDispatcher;
            return this;
        }

        public Builder<T, E, C> commandDispatcher(CommandDispatcher<T, E, C> commandDispatcher) {
            this.commandDispatcher = commandDispatcher;
            return this;
        }

        public Builder<T, E, C> deadlineDispatcher(DeadlineDispatcher<T, E> deadlineDispatcher) {
            this.deadlineDispatcher = deadlineDispatcher;
            return this;
        }

        public AggregateInvocationConfig<T, E, C> build() {
            if (Utils.anyNull(aggregateSupplier, eventDispatcher, commandDispatcher, deadlineDispatcher)) {
                final var autoConfig = AutoAggregateInvocationConfig.of(
                        requireNonNull(aggregateClass, "aggregateClass may not be null"),
                        requireNonNull(eventBaseClass, "eventBaseClass may not be null"),
                        requireNonNull(commandBaseClass, "commandBaseClass may not be null")
                );
                aggregateName = requireNonNullElse(aggregateName, autoConfig.aggregateName());
                aggregateSupplier = requireNonNullElse(aggregateSupplier, autoConfig.aggregateSupplier());
                eventDispatcher = requireNonNullElse(eventDispatcher, autoConfig.eventDispatcher());
                commandDispatcher = requireNonNullElse(commandDispatcher, autoConfig.commandDispatcher());
                deadlineDispatcher = requireNonNullElse(deadlineDispatcher, autoConfig.deadlineDispatcher());
            }
            return new AggregateInvocationConfig<>(
                    aggregateName,
                    aggregateSupplier,
                    eventDispatcher,
                    commandDispatcher,
                    deadlineDispatcher
            );
        }

    }

}
