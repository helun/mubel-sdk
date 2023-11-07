package io.mubel.sdk.fixtures;

public sealed interface TestCommands {

    record CommandA(String value) implements TestCommands {
    }

    record CommandB() implements TestCommands {
    }
}
