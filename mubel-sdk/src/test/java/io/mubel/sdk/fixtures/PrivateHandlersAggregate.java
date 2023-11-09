package io.mubel.sdk.fixtures;

import java.util.List;

/**
 * Same as {@link TestAggregate} but with private handlers.
 */
public class PrivateHandlersAggregate {

    private int processedEventCount = 0;

    public void apply(TestEvents event) {
        switch (event) {
            case TestEvents.EventA eventA -> onEvent(eventA);
            case TestEvents.EventB eventB -> onEvent(eventB);
        }
    }

    private void onEvent(TestEvents.EventA event) {
        processedEventCount = event.processedEventCount();
    }

    private void onEvent(TestEvents.EventB event) {
        processedEventCount = event.processedEventCount();
    }

    public List<TestEvents> handle(TestCommands command) {
        return switch (command) {
            case TestCommands.CommandA ca -> onCommand(ca);
            case TestCommands.CommandB cb -> onCommand(cb);
            case TestCommands.CommandC cc -> List.of();
        };
    }

    private List<TestEvents> onCommand(TestCommands.CommandA commandA) {
        return List.of(new TestEvents.EventA(commandA.value(), ++processedEventCount));
    }

    private List<TestEvents> onCommand(TestCommands.CommandB commandB) {
        return List.of(new TestEvents.EventB("B", ++processedEventCount));
    }

}
