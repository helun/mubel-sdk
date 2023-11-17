package io.mubel.sdk.scheduled;

import io.mubel.api.grpc.ScheduledEvent;

import java.util.concurrent.BlockingQueue;

public class ScheduledEventsSubscription {

    private final BlockingQueue<ScheduledEvent> buffer;

    public ScheduledEventsSubscription(BlockingQueue<ScheduledEvent> buffer) {
        this.buffer = buffer;
    }

    public ScheduledEvent next() throws InterruptedException {
        return buffer.take();
    }

}
