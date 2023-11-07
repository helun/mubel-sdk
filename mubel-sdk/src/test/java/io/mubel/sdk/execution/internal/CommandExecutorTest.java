package io.mubel.sdk.execution.internal;

import io.mubel.sdk.execution.AggregateInvocationConfig;
import io.mubel.sdk.fixtures.TestAggregate;
import io.mubel.sdk.fixtures.TestCommands;
import io.mubel.sdk.fixtures.TestEvents;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandExecutorTest {

    @Test
    void doit() {
        final var config = AggregateInvocationConfig.of(
                TestAggregate::new,
                a -> a::apply,
                (TestAggregate a) -> a::handle
        );

        final var result = new CommandExecutor<>(config)
                .execute(List.of(), new TestCommands.CommandA("test"));

        assertThat(result)
                .hasSize(1)
                .contains(new TestEvents.EventA("test", 1));
    }

}