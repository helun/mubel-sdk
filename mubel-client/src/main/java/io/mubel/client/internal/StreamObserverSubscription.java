package io.mubel.client.internal;

import io.grpc.stub.StreamObserver;
import io.mubel.api.grpc.EventData;
import io.mubel.client.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class StreamObserverSubscription implements Subscription, StreamObserver<EventData> {

    private final BlockingQueue<EventData> buffer;
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private final AtomicBoolean completed = new AtomicBoolean(false);

    public StreamObserverSubscription(int bufferSize) {
        this.buffer = new ArrayBlockingQueue<>(bufferSize);
    }

    @Override
    public void onNext(EventData value) {
        try {
            while (!buffer.offer(value, 1, TimeUnit.MINUTES)) {
                // keep trying
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onError(Throwable t) {
        error.set(t);
    }

    @Override
    public void onCompleted() {
        completed.set(true);
    }

    @Override
    public EventData next() throws InterruptedException {
        checkErrors();
        if (completed.get()) {
            return null;
        }
        return buffer.take();
    }

    @Override
    public List<EventData> nextBatch(int size) throws InterruptedException {
        checkErrors();
        final var batch = new ArrayList<EventData>(size);
        if (completed.get()) {
            return batch;
        }
        buffer.drainTo(batch, size);
        if (batch.isEmpty()) {
            batch.add(buffer.take());
            buffer.drainTo(batch, size - 1);
        }
        return batch;
    }

    private void checkErrors() {
        if (errorHasOccurred()) {
            throwError();
        }
    }

    private void throwError() {
        final var e = error.get();
        if (e instanceof RuntimeException rte) {
            throw rte;
        }
        throw new RuntimeException(error.get());
    }

    private boolean errorHasOccurred() {
        return error.get() != null;
    }
}
