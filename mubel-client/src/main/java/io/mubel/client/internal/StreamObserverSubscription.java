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
import java.util.function.Function;

public class StreamObserverSubscription<T> implements Subscription<T>, StreamObserver<T> {

    private final BlockingQueue<T> buffer;
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final Function<T, T> eventErrorChecker;

    public StreamObserverSubscription(int bufferSize, Function<T, T> eventErrorChecker) {
        this.buffer = new ArrayBlockingQueue<>(bufferSize);
        this.eventErrorChecker = eventErrorChecker;
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
        if (completed.get()) {
            return eventErrorChecker.apply(buffer.poll());
        }
        return eventErrorChecker.apply(buffer.take());
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
            eventErrorChecker.apply(e);
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
