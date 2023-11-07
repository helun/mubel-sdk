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