package io.mubel.client.internal;

import io.grpc.stub.StreamObserver;
import io.mubel.client.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class StreamObserverSubscription<T> implements Subscription<T>, StreamObserver<T> {

    private final BlockingQueue<T> buffer;
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final Predicate<T> systemEventFilter;

    public StreamObserverSubscription(int bufferSize, Predicate<T> systemEventFilter) {
        this.buffer = new ArrayBlockingQueue<>(bufferSize);
        this.systemEventFilter = systemEventFilter;
    }

    @Override
    public void onNext(T value) {
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
    public T next() throws InterruptedException {
        checkErrors();
        T event;
        if (completed.get()) {
            event = buffer.poll();
        } else {
            event = buffer.take();
        }
        if (!systemEventFilter.test(event)) {
            return next();
        }
        return event;
    }

    @Override
    public List<T> nextBatch(int size) throws InterruptedException {
        checkErrors();
        final var batch = new ArrayList<T>(size);
        buffer.drainTo(batch, size);
        if (batch.isEmpty() && !completed.get()) {
            batch.add(buffer.take());
            buffer.drainTo(batch, size - 1);
        }
        for (final var e : batch) {
            if (!systemEventFilter.test(e)) {
                final var filtered = filterSystemEvents(batch);
                if (filtered.isEmpty()) {
                    return nextBatch(size);
                } else {
                    return filtered;
                }
            }
        }
        return batch;
    }

    private List<T> filterSystemEvents(List<T> batch) {
        final var filtered = new ArrayList<T>(batch.size());
        for (final var e : batch) {
            if (systemEventFilter.test(e)) {
                filtered.add(e);
            }
        }
        return filtered;
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
