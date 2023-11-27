package io.mubel.fixtures;

import io.mubel.sdk.Deadline;
import io.mubel.sdk.HandlerResult;
import io.mubel.sdk.annotation.CommandHandler;
import io.mubel.sdk.annotation.DeadlineHandler;
import io.mubel.sdk.annotation.EventHandler;
import io.mubel.sdk.scheduled.ExpiredDeadline;

import java.util.List;
import java.util.UUID;

public class TestAggregate {

    private int processedEventCount = 0;
    private UUID deadlineId;

    @EventHandler
    public void apply(TestEvents event) {
        switch (event) {
            case TestEvents.EventA eventA -> onEvent(eventA);
            case TestEvents.EventB eventB -> onEvent(eventB);
            case TestEvents.DeadlineExpired deadlineExpired -> onEvent(deadlineExpired);
            case TestEvents.EventWithDeadlineRef eventWithDeadlineRef -> onEvent(eventWithDeadlineRef);
        }
    }

    public void onEvent(TestEvents.EventA event) {
        processedEventCount = event.processedEventCount();
    }

    public void onEvent(TestEvents.EventB event) {
        processedEventCount = event.processedEventCount();
    }

    private void onEvent(TestEvents.EventWithDeadlineRef eventWithDeadlineRef) {
        deadlineId = eventWithDeadlineRef.deadlineId();
    }

    private void onEvent(TestEvents.DeadlineExpired deadlineExpired) {
        // do nothing
    }

    @DeadlineHandler
    public TestEvents handle(ExpiredDeadline deadline) {
        return new TestEvents.DeadlineExpired(deadline.deadlineName());
    }

    @CommandHandler
    public List<TestEvents> onCommand(TestCommands.EmptyResultCommand erc) {
        return List.of();
    }

    @CommandHandler
    public List<TestEvents> onCommand(TestCommands.ReturnNullCommand rnc) {
        return null;
    }

    @CommandHandler
    public HandlerResult<TestEvents> onCommand(TestCommands.DeadlineSchedulingCommand basicCommand) {
        var deadline = Deadline.ofSeconds("deadline", 1);
        return HandlerResult.<TestEvents>of(new TestEvents.EventWithDeadlineRef(deadline.id()))
                .deadline(deadline)
                .build();
    }

    @CommandHandler
    public HandlerResult<TestEvents> onCommand(TestCommands.BasicCommand basicCommand) {
        return HandlerResult.<TestEvents>of(new TestEvents.EventA(
                        basicCommand.value(),
                        ++processedEventCount
                ))
                .build();
    }

    @CommandHandler
    public HandlerResult<TestEvents> onCommand(TestCommands.DeadlineCancellingCommand dcc) {
        return HandlerResult.<TestEvents>of(new TestEvents.EventB("value", ++processedEventCount))
                .cancel(deadlineId.toString())
                .build();
    }

    public int processedEventCount() {
        return processedEventCount;
    }

}
