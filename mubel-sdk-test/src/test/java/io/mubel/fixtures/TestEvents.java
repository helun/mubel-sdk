package io.mubel.fixtures;

public sealed interface TestEvents {

    record EventA(String value, int processedEventCount) implements TestEvents {
    }

    record EventB(String value, int processedEventCount) implements TestEvents {
    }
}
