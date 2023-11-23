package io.mubel.sdk.fixtures;

import io.mubel.sdk.HandlerResult;
import io.mubel.sdk.annotation.CommandHandler;
import io.mubel.sdk.annotation.DeadlineHandler;
import io.mubel.sdk.annotation.EventHandler;

import java.util.List;
import java.util.UUID;

public class TestAggregate {

    private int processedEventCount = 0;

    public int getProcessedEventCount() {
        return processedEventCount;
    }

    @EventHandler
    public void onEvent(TestEvents.EventA event) {
        processedEventCount = event.processedEventCount();
    }

    @EventHandler
    public void onEvent(TestEvents.EventB event) {
        processedEventCount = event.processedEventCount();
    }

    @CommandHandler
    public List<TestEvents> onCommand(TestCommands.CommandC cc) {
        return List.of(new TestEvents.EventA("C", ++processedEventCount));
    }

    @CommandHandler
    public List<TestEvents> onCommand(TestCommands.CommandA commandA) {
        return List.of(new TestEvents.EventA(commandA.value(), ++processedEventCount));
    }

    @CommandHandler
    public List<TestEvents> onCommand(TestCommands.CommandB commandB) {
        return List.of(new TestEvents.EventB("B", ++processedEventCount, UUID.randomUUID()));
    }

    @DeadlineHandler
    public HandlerResult<TestEvents> onDeadline() {
        return HandlerResult.<TestEvents>of(new TestEvents.EventA("deadline", ++processedEventCount))
                .build();
    }

    @DeadlineHandler("named-deadline")
    public HandlerResult<TestEvents> onNamedDeadline() {
        return HandlerResult.<TestEvents>of(new TestEvents.EventA("deadline", ++processedEventCount))
                .build();
    }

}
