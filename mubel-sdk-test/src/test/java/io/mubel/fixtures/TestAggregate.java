package io.mubel.fixtures;

import java.util.List;

public class TestAggregate {

    private int processedEventCount = 0;

    public void apply(TestEvents event) {
        switch (event) {
            case TestEvents.EventA eventA -> onEvent(eventA);
            case TestEvents.EventB eventB -> onEvent(eventB);
        }
    }

    public void onEvent(TestEvents.EventA event) {
        processedEventCount = event.processedEventCount();
    }

    public void onEvent(TestEvents.EventB event) {
        processedEventCount = event.processedEventCount();
    }

    public List<TestEvents> handle(TestCommands command) {
        return switch (command) {
            case TestCommands.CommandA commandA -> onCommand(commandA);
            case TestCommands.ReturnNullCommand rnc -> onCommand(rnc);
            case TestCommands.EmptyResultCommand erc -> onCommand(erc);
        };
    }

    public List<TestEvents> onCommand(TestCommands.EmptyResultCommand erc) {
        return List.of();
    }

    public List<TestEvents> onCommand(TestCommands.ReturnNullCommand rnc) {
        return null;
    }

    public List<TestEvents> onCommand(TestCommands.CommandA commandA) {
        return List.of(new TestEvents.EventA(commandA.value(), ++processedEventCount));
    }

    public int processedEventCount() {
        return processedEventCount;
    }

}
