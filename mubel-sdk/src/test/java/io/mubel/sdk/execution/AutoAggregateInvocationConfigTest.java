package io.mubel.sdk.execution;

import io.mubel.sdk.execution.internal.CommandExecutor;
import io.mubel.sdk.fixtures.PrivateHandlersAggregate;
import io.mubel.sdk.fixtures.TestAggregate;
import io.mubel.sdk.fixtures.TestCommands;
import io.mubel.sdk.fixtures.TestEvents;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AutoAggregateInvocationConfigTest {

    @Test
    void autoConfig() {
        final var commandExecutor = new CommandExecutor<>(AutoAggregateInvocationConfig.of(
                TestAggregate.class,
                TestEvents.class,
                TestCommands.class
        ));
        final var events = commandExecutor.execute(
                List.of(new TestEvents.EventA("value", 0)),
                new TestCommands.CommandA("value")
        );
        assertThat(events)
                .hasSize(1)
                .first()
                .satisfies(event -> {
                    assertThat(event).isInstanceOf(TestEvents.EventA.class);
                    TestEvents.EventA eventA = (TestEvents.EventA) event;
                    assertThat(eventA.processedEventCount()).isEqualTo(1);
                });
    }

    @Test
    void autoConfigWithPrivateHandlers() {
        final var commandExecutor = new CommandExecutor<>(AutoAggregateInvocationConfig.of(
                PrivateHandlersAggregate.class,
                TestEvents.class,
                TestCommands.class
        ));
        final var events = commandExecutor.execute(
                List.of(new TestEvents.EventA("value", 0)),
                new TestCommands.CommandA("value")
        );
        assertThat(events)
                .hasSize(1)
                .first()
                .satisfies(event -> {
                    assertThat(event).isInstanceOf(TestEvents.EventA.class);
                    TestEvents.EventA eventA = (TestEvents.EventA) event;
                    assertThat(eventA.processedEventCount()).isEqualTo(1);
                });
    }

}