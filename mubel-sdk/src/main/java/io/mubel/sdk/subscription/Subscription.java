package io.mubel.sdk.subscription;

import io.mubel.api.grpc.EventData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static java.util.Objects.requireNonNull;

public class Subscription {
    private final BlockingQueue<EventData> buffer;

    public Subscription(BlockingQueue<EventData> buffer) {
        this.buffer = requireNonNull(buffer, "buffer may not be null");
    }

    public List<EventData> nextBatch(int size) throws InterruptedException {
        final var batch = new ArrayList<EventData>(size);
        buffer.drainTo(batch, size);
        if (batch.isEmpty()) {
            batch.add(buffer.take());
            buffer.drainTo(batch, size - 1);
        }
        return batch;
    }

}
