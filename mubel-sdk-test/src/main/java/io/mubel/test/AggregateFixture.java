package io.mubel.test;

import io.mubel.sdk.Deadline;
import io.mubel.sdk.HandlerResult;
import io.mubel.sdk.execution.AggregateInvocationConfig;
import io.mubel.test.internal.AggregateTestExecutor;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class AggregateFixture<T> {

    private final AggregateTestExecutor<T> executor;


    public AggregateFixture(AggregateInvocationConfig<T, Object, Object> config) {
        this.executor = new AggregateTestExecutor<>(config);
    }

    public AggregateFixture<T> givenHandlerResult(HandlerResult<?> result) {
        return this;
    }

    public AggregateFixture<T> given(Object... events) {
        executor.applyEvents(Arrays.asList(events));
        return this;
    }

    public AggregateFixture<T> given(List<Object> events) {
        executor.applyEvents(events);
        return this;
    }

    /**
     * Given a list of commands, execute them and apply the resulting events to the aggregate
     * Any events generated by the commands will not be stored in the fixture,
     * i.e. they will not show up in expectEvents or expectEventCount
     *
     * @param commands
     * @return
     */
    public AggregateFixture<T> givenCommands(Object... commands) {
        for (final var command : commands) {
            executor.applyCommand(command);
        }
        return this;
    }

    /**
     * Given a list of commands, execute them and apply the resulting events to the aggregate
     * Any events generated by the commands will not be stored in the fixture,
     * i.e. they will not show up in expectEvents or expectEventCount
     *
     * @param commands
     * @return
     */
    public AggregateFixture<T> givenCommands(List<Object> commands) {
        for (final var command : commands) {
            executor.applyCommand(command);
        }
        return this;
    }

    /**
     * Execute a command and apply the resulting events to the aggregate
     * Any events generated by the command can be asserted on using expectEvents or expectEventCount
     *
     * @param command
     * @return
     */
    public AggregateFixture<T> when(Object command) {
        executor.setCheckPoint();
        executor.applyAndRecordCommand(command);
        return this;
    }

    public AggregateFixture<T> whenTimeElapses(Duration duration) {
        executor.setCheckPoint();
        executor.advanceTimeBy(duration);
        return this;
    }

    public AggregateFixture<T> expectSuccessfulHandlerExecution() {
        assertCommandHasBeenExecuted();
        return this;
    }

    /**
     * Assert that the command handler returned the expected events
     *
     * @param events
     * @return
     */
    public AggregateFixture<T> expectEvents(Object... events) {
        assertCommandHasBeenExecuted();
        assertThat("expectEvents", executor.events(), hasItems(events));
        return this;
    }

    /**
     * Assert that the command handler did not produce any events
     *
     * @return
     */
    public AggregateFixture<T> expectNoEvents() {
        assertCommandHasBeenExecuted();
        assertThat("expectNoEvents", executor.events(), hasSize(0));
        return this;
    }

    /**
     * Assert that the command handler produced the expected number of events
     *
     * @param count
     * @return
     */
    public AggregateFixture<T> expectEventCount(int count) {
        assertCommandHasBeenExecuted();
        assertThat("expected event count", executor.events(), hasSize(count));
        return this;
    }

    /**
     * Another way to assert on the events produced by the command handler
     * Useful when you what to use other assertion methods than simple equality
     *
     * @param eventConsumer
     * @return
     */
    public AggregateFixture<T> expectEventsSatisfies(Consumer<List<?>> eventConsumer) {
        assertCommandHasBeenExecuted();
        eventConsumer.accept(executor.events());
        return this;
    }

    public AggregateFixture<T> expectNoDeadlines() {
        assertCommandHasBeenExecuted();
        assertThat("expectNoDeadlines", executor.deadlines(), hasSize(0));
        return this;
    }

    public AggregateFixture<T> expectDeadlineCount(int count) {
        assertCommandHasBeenExecuted();
        assertThat("expected deadline count", executor.deadlines(), hasSize(count));
        return this;
    }

    public AggregateFixture<T> expectDeadlinesSatisfies(Consumer<List<Deadline>> deadlineConsumer) {
        assertCommandHasBeenExecuted();
        deadlineConsumer.accept(executor.deadlines());
        return this;
    }

    /**
     * Assert that at least one of the events produced by the command handler satisfies the given condition verified by the eventConsumer
     *
     * @param eventConsumer
     * @return
     */
    public AggregateFixture<T> expectAnyEventSatisfies(Consumer<Object> eventConsumer) {
        assertCommandHasBeenExecuted();
        final var events = executor.events();
        assertThat("at least one event is expected", events, hasSize(greaterThan(0)));
        AssertionError lastError = null;
        boolean success = false;
        for (final var event : events) {
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

    public AggregateFixture<T> expectLastEventSatisfies(Consumer<Object> eventConsumer) {
        assertCommandHasBeenExecuted();
        final var events = executor.events();
        assertThat("at least one event is expected", events, hasSize(greaterThan(0)));
        eventConsumer.accept(events.getLast());
        return this;
    }

    /**
     * Assert that the aggregate state satisfies the given condition verified by the stateConsumer
     *
     * @param condition
     * @return
     */
    public AggregateFixture<T> state(Consumer<T> condition) {
        condition.accept(executor.state());
        return this;
    }

    private void assertCommandHasBeenExecuted() {
        assertThat("No command has been executed", executor.executionCount(), greaterThan(0));
    }

}
