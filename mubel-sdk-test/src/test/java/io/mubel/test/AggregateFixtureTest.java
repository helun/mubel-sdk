package io.mubel.test;

import io.mubel.fixtures.TestAggregate;
import io.mubel.fixtures.TestCommands;
import io.mubel.fixtures.TestEvents;
import io.mubel.sdk.exceptions.NoCommandHandlerFoundException;
import io.mubel.sdk.execution.AutoAggregateInvocationConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AggregateFixtureTest {

    AggregateFixture<TestAggregate> fixture = new AggregateFixture<>(
            AutoAggregateInvocationConfig.of(TestAggregate.class)
    );
    TestCommands.CommandA command = new TestCommands.CommandA("a value");

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
                .hasMessageContaining("expectEventCount");
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
                .expectEvents(new TestEvents.EventA("a value", 2));
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
        ).isInstanceOf(NoCommandHandlerFoundException.class)
                .hasMessageContaining("No command handler found for class io.mubel.fixtures.TestCommands$ReturnNullCommand");
    }

}