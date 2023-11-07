package io.mubel.fixtures;

public sealed interface TestCommands {

    record CommandA(String value) implements TestCommands {
    }

    record ReturnNullCommand() implements TestCommands {}

    record EmptyResultCommand() implements TestCommands {}
}
