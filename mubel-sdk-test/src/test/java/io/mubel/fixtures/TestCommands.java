package io.mubel.fixtures;

public sealed interface TestCommands {

    record BasicCommand(String value) implements TestCommands {
    }

    record ReturnNullCommand() implements TestCommands {
    }

    record EmptyResultCommand() implements TestCommands {
    }

    record DeadlineSchedulingCommand() implements TestCommands {
    }

    record DeadlineCancellingCommand() implements TestCommands {
    }
}
