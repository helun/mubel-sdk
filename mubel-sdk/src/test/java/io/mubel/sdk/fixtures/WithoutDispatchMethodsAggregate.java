package io.mubel.sdk.fixtures;

import java.util.List;

public class WithoutDispatchMethodsAggregate {

    private int processedEventCount = 0;

    public TestEvents methodNameDoesNotMatter(TestCommands.CommandC cc) {
        return new TestEvents.EventA("C", ++processedEventCount);
    }

    public List<TestEvents> handle(TestCommands.CommandA commandA) {
        return List.of(new TestEvents.EventA(commandA.value(), ++processedEventCount));
    }

    public TestEvents doCommandC(TestCommands.CommandB commandB) {
        return new TestEvents.EventB("B", ++processedEventCount);
    }
}
