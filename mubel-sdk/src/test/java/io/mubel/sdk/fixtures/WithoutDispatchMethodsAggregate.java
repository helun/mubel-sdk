package io.mubel.sdk.fixtures;

import io.mubel.sdk.Deadline;
import io.mubel.sdk.HandlerResult;
import io.mubel.sdk.annotation.CommandHandler;

import java.util.List;

public class WithoutDispatchMethodsAggregate {

    private int processedEventCount = 0;

    @CommandHandler
    public TestEvents methodNameDoesNotMatter(TestCommands.CommandC cc) {
        return new TestEvents.EventA("C", ++processedEventCount);
    }

    @CommandHandler
    public List<TestEvents> handle(TestCommands.CommandA commandA) {
        return List.of(new TestEvents.EventA(commandA.value(), ++processedEventCount));
    }

    @CommandHandler
    public HandlerResult<TestEvents> handleCommandB(TestCommands.CommandB commandB) {
        final var deadline = Deadline.ofSeconds("reservation", 10);
        final var deadlineId = deadline.id();
        final TestEvents event = new TestEvents.EventB(
                "B",
                ++processedEventCount,
                deadlineId
        );
        return HandlerResult.of(event)
                .deadline(deadline)
                .build();
    }

}
