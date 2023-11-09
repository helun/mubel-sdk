package io.mubel.sdk.reflection;

import io.mubel.sdk.fixtures.PrivateHandlersAggregate;
import io.mubel.sdk.fixtures.TestAggregate;
import io.mubel.sdk.fixtures.TestCommands;
import io.mubel.sdk.fixtures.TestEvents;
import io.mubel.sdk.internal.reflection.AggregateClassUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateClassUtilTest {

    @Test
    void newInstance() {
        final var instance = AggregateClassUtil.newInstance(TestAggregate.class);
        assertThat(instance).isInstanceOf(TestAggregate.class);
    }

    @Test
    void findCommandHandlers() {
        assertThat(AggregateClassUtil.findCommandHandler(
                TestAggregate.class,
                TestCommands.CommandA.class)
        ).isNotEmpty();
        assertThat(AggregateClassUtil.findCommandHandler(
                TestAggregate.class,
                TestCommands.CommandB.class)
        ).isNotEmpty();
        assertThat(AggregateClassUtil.findCommandHandler(
                TestAggregate.class,
                TestCommands.CommandA.class)
        ).isNotEmpty();
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


}