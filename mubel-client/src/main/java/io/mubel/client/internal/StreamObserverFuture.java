package io.mubel.client.internal;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;

public class StreamObserverFuture<T> implements StreamObserver<T> {

    private final CompletableFuture<T> future;

    public StreamObserverFuture(CompletableFuture<T> future) {
        this.future = future;
    }

    @Override
    public void onNext(T value) {
        future.complete(value);
    }

    @Override
    public void onError(Throwable t) {
        future.completeExceptionally(t);
    }

    @Override
    public void onCompleted() {

    }
}
