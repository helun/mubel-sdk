package io.mubel.sdk.subscription;

import io.mubel.api.grpc.EventData;
import io.mubel.sdk.fixtures.Fixtures;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SubscriptionTest {

    final BlockingQueue<EventData> buffer = new ArrayBlockingQueue<>(100);

    final Subscription subscription = new Subscription(buffer);

    @Test
    void nextBatch() throws InterruptedException {
        var ed1 = newEventData();
        var ed2 = newEventData();
        var ed3 = newEventData();

        send(ed1, ed2, ed3);

        final var result = subscription.nextBatch(100);
        assertThat(result.getLast().getSequenceNo()).isEqualTo(ed3.getSequenceNo());
        assertThat(result)
                .hasSize(3)
                .map(EventData::getId)
                .containsExactly(ed1.getId(), ed2.getId(), ed3.getId());
    }
    
    @Test
    void waitOnEvents() throws Exception {
        Future<List<EventData>> batchFuture = Executors.newSingleThreadExecutor()
                .submit(() -> subscription.nextBatch(100));

        var ed1 = newEventData();
        var ed2 = newEventData();
        var ed3 = newEventData();

        send(ed1, ed2, ed3);
        await().until(batchFuture::isDone);
        assertThat(batchFuture.get())
                .hasSize(3);
    }

    @Test
    void nextBatchWhenBufferIsEmpty() throws Exception {
        var batchFuture = Executors.newSingleThreadExecutor()
                .submit(() -> subscription.nextBatch(100));

        send(newEventData(), newEventData());

        await().until(batchFuture::isDone);
        assertThat(batchFuture.get()).hasSizeGreaterThan(0);
    }

    private static EventData newEventData() {
        return Fixtures.eventDataBuilder().build();
    }

    private void send(EventData... eventData) {
        Collections.addAll(buffer, eventData);
    }

}