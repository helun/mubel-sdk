package io.mubel.test;

import io.mubel.fixtures.TestAggregate;
import io.mubel.fixtures.TestCommands;
import io.mubel.fixtures.TestEvents;
import io.mubel.sdk.Deadline;
import io.mubel.sdk.exceptions.CommandHandlerException;
import io.mubel.sdk.execution.AutoAggregateInvocationConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AggregateFixtureTest {

    AggregateFixture<TestAggregate> fixture = new AggregateFixture<>(
            AutoAggregateInvocationConfig.of(TestAggregate.class)
    );
    TestCommands.BasicCommand command = new TestCommands.BasicCommand("a value");

    @Test
    void expectEvents() {
        fixture.when(command)
                .expectEvents(new TestEvents.EventA("a value", 1));
    }

    @Test
    void expectEventCount() {
        fixture.when(command)
                .expectEventCount(1);
    }

    @Test
    void expectEventCountFails() {
        assertThatThrownBy(() -> fixture.when(command)
                .expectEventCount(0))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("expected event count");
    }

    @Test
    void expectEventsSatisfies() {
        fixture.when(command)
                .expectEventsSatisfies(events -> assertThat(events)
                        .hasSize(1)
                        .first()
                        .satisfies(event -> {
                            assertThat(event).isInstanceOf(TestEvents.EventA.class);
                            final var eventA = (TestEvents.EventA) event;
                            assertThat(eventA.processedEventCount()).isEqualTo(1);
                        }));
    }

    @Test
    void expectEventsSatisfiesFails() {
        assertThatThrownBy(() ->
                fixture.when(command)
                        .expectEventsSatisfies(events ->
                                assertThat(events)
                                        .hasSize(3)
                        )
        );
    }

    @Test
    void expectAnyEventSatisfies() {
        fixture.when(command)
                .expectAnyEventSatisfies(event -> {
                    assertThat(event).isInstanceOf(TestEvents.EventA.class);
                    final var eventA = (TestEvents.EventA) event;
                    assertThat(eventA.processedEventCount()).isEqualTo(1);
                });
    }

    @Test
    void givenEvents() {
        fixture.given(new TestEvents.EventA("A", 1))
                .when(command)
                .expectEvents(new TestEvents.EventA("a value", 2))
                .state(state -> assertThat(state.processedEventCount())
                        .as("state processed event count")
                        .isEqualTo(2));
    }

    @Test
    void givenCommands() {
        fixture.givenCommands(command)
                .state(state -> assertThat(state.processedEventCount())
                        .as("state processed event count")
                        .isEqualTo(1));
    }

    @Test
    void state() {
        fixture.given(new TestEvents.EventA("A", 1))
                .state(state -> assertThat(state.processedEventCount())
                        .as("processed event count")
                        .isEqualTo(1));
    }

    @Test
    void stateFails() {
        assertThatThrownBy(() -> fixture.given(new TestEvents.EventA("A", 1))
                .state(state -> assertThat(state.processedEventCount())
                        .as("processed event count")
                        .isEqualTo(0)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("processed event count");
    }

    @Test
    void givenEventsList() {
        fixture.given(List.of(new TestEvents.EventA("A", 1)))
                .when(command)
                .expectEvents(new TestEvents.EventA("a value", 2));
    }

    @Test
    void expectedEventsFail() {
        assertThatThrownBy(() -> fixture.when(command)
                .expectEvents(new TestEvents.EventA("a value", 0))).isInstanceOf(java.lang.AssertionError.class)
                .hasMessageContaining("expectEvents");
    }

    @Test
    void expectNoEvents() {
        fixture.when(new TestCommands.EmptyResultCommand())
                .expectSuccessfulHandlerExecution()
                .expectNoEvents();
    }

    @Test
    void commandHandlerReturnsNull() {
        assertThatThrownBy(() ->
                fixture.when(new TestCommands.ReturnNullCommand())
                        .expectEvents(new TestEvents.EventA("a value", 0))
        ).isInstanceOf(CommandHandlerException.class)
                .hasMessageContaining("Caught exception while invoking");
    }

    @Test
    void timeElapses() {
        fixture.when(new TestCommands.DeadlineSchedulingCommand())
                .expectDeadlinesSatisfies(deadlines -> assertThat(deadlines)
                        .hasSize(1)
                        .first()
                        .extracting(Deadline::name)
                        .isEqualTo("deadline"))
                .whenTimeElapses(Duration.ofSeconds(1))
                .expectLastEventSatisfies(event -> assertThat(event)
                        .isInstanceOf(TestEvents.DeadlineExpired.class)
                        .extracting("value")
                        .isEqualTo("deadline"));
    }

    @Test
    void deadlineCancelled() {
        fixture.when(new TestCommands.DeadlineSchedulingCommand())
                .expectDeadlineCount(1)
                .expectLastEventSatisfies(event -> assertThat(event).isInstanceOf(TestEvents.EventWithDeadlineRef.class))
                .when(new TestCommands.DeadlineCancellingCommand())
                .whenTimeElapses(Duration.ofSeconds(1))
                .expectNoEvents();
    }

}