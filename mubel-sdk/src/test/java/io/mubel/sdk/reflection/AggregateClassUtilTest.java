package io.mubel.sdk.reflection;

import io.mubel.sdk.fixtures.*;
import io.mubel.sdk.internal.reflection.AggregateClassUtil;
import io.mubel.sdk.scheduled.ExpiredDeadline;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.mubel.sdk.annotation.DeadlineHandler.DEFAULT_DEADLINE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

class AggregateClassUtilTest {

    @Test
    void newInstance() {
        final var instance = AggregateClassUtil.newInstance(TestAggregate.class);
        assertThat(instance).isInstanceOf(TestAggregate.class);
    }

    @Test
    void findingEventHandlerAndCommandHandlerShouldNotAffectEachOther() {
        assertThat(AggregateClassUtil.findEventHandler(
                TestAggregate.class,
                TestCommands.CommandA.class)
        ).isEmpty();
        assertThat(AggregateClassUtil.findCommandHandler(
                TestAggregate.class,
                TestCommands.CommandA.class)
        ).as("Failure to find eventHandler with a command class should not affect a later lookup of correct command handler")
                .isNotEmpty();
    }

    @Test
    void findCommandHandler() {
        assertThat(AggregateClassUtil.findCommandHandler(
                TestAggregate.class,
                TestCommands.CommandA.class)
        ).isNotEmpty();
    }

    @Test
    void findCommandHandlerInPrivateHandlersAggregate() {
        assertThat(AggregateClassUtil.findCommandHandler(
                PrivateHandlersAggregate.class,
                TestCommands.CommandA.class)
        ).isNotEmpty();
    }

    @Test
    void findAnotherCommandHandler() {
        assertThat(AggregateClassUtil.findCommandHandler(
                TestAggregate.class,
                TestCommands.CommandB.class)
        ).isNotEmpty();
    }

    @Test
    void findCommandHandlerWithNonListReturnType() {
        assertThat(AggregateClassUtil.findCommandHandler(
                WithoutDispatchMethodsAggregate.class,
                TestCommands.CommandC.class)
        ).isNotEmpty();
    }

    @Test
    void commandHandlerDoesNotExists() {
        assertThat(AggregateClassUtil.findCommandHandler(
                TestAggregate.class,
                String.class)
        ).isEmpty();
    }

    @Test
    void findEventHandler() {
        assertThat(AggregateClassUtil.findEventHandler(
                TestAggregate.class,
                TestEvents.EventA.class)
        ).isNotEmpty();
    }

    @Test
    void eventHandlerDoesNotExists() {
        assertThat(AggregateClassUtil.findEventHandler(
                TestAggregate.class,
                String.class)
        ).isEmpty();
        assertThat(AggregateClassUtil.findEventHandler(
                TestAggregate.class,
                TestCommands.CommandA.class)
        ).isEmpty();
    }

    @Test
    void findEventHandlerInPrivateHandlersAggregate() {
        assertThat(AggregateClassUtil.findEventHandler(
                PrivateHandlersAggregate.class,
                TestEvents.EventA.class)
        ).isNotEmpty();
    }

    @Test
    void findConstructor() {
        assertThat(AggregateClassUtil.findConstructor(
                TestAggregate.class)
        ).isNotEmpty();
    }

    @Test
    void catchUnmatchedNameDeadlineHandler() {
        assertThat(AggregateClassUtil.findDeadlineHandler(
                TestAggregate.class,
                new ExpiredDeadline(UUID.randomUUID(), "test-deadline", Map.of(), Instant.now()))
        ).hasValueSatisfying(method -> assertThat(method.getName())
                .isEqualTo("onDeadline")
        );
    }

    @Test
    void allDeadlineHandler() {
        assertThat(AggregateClassUtil.findDeadlineHandler(
                TestAggregate.class,
                new ExpiredDeadline(UUID.randomUUID(), DEFAULT_DEADLINE_NAME, Map.of(), Instant.now()))
        ).hasValueSatisfying(method -> assertThat(method.getName())
                .isEqualTo("onDeadline")
        );
    }

    @Test
    void namedDeadlineHandler() {
        assertThat(AggregateClassUtil.findDeadlineHandler(
                TestAggregate.class,
                new ExpiredDeadline(UUID.randomUUID(), "named-deadline", Map.of(), Instant.now()))
        ).hasValueSatisfying(method -> assertThat(method.getName())
                .isEqualTo("onNamedDeadline")
        );
    }

    @Test
    void deadlineHandlerDoesNotExists() {
        assertThat(AggregateClassUtil.findDeadlineHandler(
                String.class,
                new ExpiredDeadline(UUID.randomUUID(), "dl name", Map.of(), Instant.now()))
        ).isEmpty();
    }

}