package io.mubel.test;

import io.mubel.sdk.execution.AggregateInvocationConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.function.Consumer;


public class AggregateFixture<T> {

    private List<Object> actualEvents;
    private final AggregateInvocationConfig<T, Object, Object> config;
    private final T state;
    private boolean commandExecuted = false;

    public AggregateFixture(AggregateInvocationConfig<T, Object, Object> config) {
        this.config = config;
        this.state = config.aggregateSupplier().get();
    }

    public AggregateFixture<T> given(Object... events) {
        return given(List.of(events));
    }

    public AggregateFixture<T> given(List<Object> events) {
        final var dispatcher = config.eventDispatcher().resolveEventHandler(state);
        for (final var event : events) {
            dispatcher.accept(event);
        }
        return this;
    }

    public AggregateFixture<T> givenCommands(Object... events) {
        return given(List.of(events));
    }

    public AggregateFixture<T> givenCommands(List<Object> events) {
        final var cmdHandler = config.commandDispatcher().resolveCommandHandler(state);
        for (final var cmd: events) {
            cmdHandler.apply(cmd);
        }
        return this;
    }

    public AggregateFixture<T> when(Object command) {
        this.actualEvents = config.commandDispatcher()
                .resolveCommandHandler(state)
                .apply(command);
        commandExecuted = true;
        return this;
    }

    public AggregateFixture<T> expectSuccessfulHandlerExecution() {
        assertCommandHasBeenExecuted();
        return this;
    }

    public AggregateFixture<T> expectEvents(Object... events) {
        assertCommandHasBeenExecuted();
        assertCommandHandlerReturnValue();
        assertThat("expectEvents", actualEvents, hasItems(events));
        return this;
    }

    public AggregateFixture<T> expectNoEvents() {
        assertCommandHasBeenExecuted();
        assertCommandHandlerReturnValue();
        assertThat("expectNoEvents", actualEvents, hasSize(0));
        return this;
    }


    public AggregateFixture<T> expectEventCount(int count) {
        assertCommandHasBeenExecuted();
        assertCommandHandlerReturnValue();
        assertThat("expectEventCount", actualEvents, hasSize(count));
        return this;
    }

    public AggregateFixture<T> expectEventsSatisfies(Consumer<List<?>> eventConsumer) {
        assertCommandHasBeenExecuted();
        assertCommandHandlerReturnValue();
        eventConsumer.accept(actualEvents);
        return this;
    }

    public AggregateFixture<T> expectAnyEventSatisfies(Consumer<Object> eventConsumer) {
        assertCommandHasBeenExecuted();
        assertCommandHandlerReturnValue();
        assertThat("at least one event is expected", actualEvents, hasSize(greaterThan(0)));
        AssertionError lastError = null;
        boolean success = false;
        for (final var event : actualEvents) {
            try {
                eventConsumer.accept(event);
                success = true;
                break;
            } catch (AssertionError e) {
                lastError = e;
            }
        }
        if (!success && lastError != null) {
            throw lastError;
        }
        return this;
    }

    private void assertCommandHandlerReturnValue() {
        assertThat("Aggregate should return a non null result", actualEvents, notNullValue());
    }

    private void assertCommandHasBeenExecuted() {
        assertThat("No command has been executed", commandExecuted);
    }

    public AggregateFixture<T> state(Consumer<T> condition) {
        condition.accept(state);
        return this;
    }
}
