package io.mubel.sdk.fixtures;

public class DuplicateHandlersAggregate {

    public TestEvents handler(TestCommands.CommandC cmd) {
        return new TestEvents.EventA("C", 0);
    }

    public TestEvents duplicateHandler(TestCommands.CommandC cmd) {
        return handler(cmd);
    }

    public void eventHandler(TestEvents e) {

    }

    public void duplicateEventHandler(TestEvents e) {

    }
}
