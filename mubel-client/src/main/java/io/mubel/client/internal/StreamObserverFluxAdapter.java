package io.mubel.client.internal;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamObserverFluxAdapter<T> implements StreamObserver<T> {

    private static final int BUFFER_SIZE = 16;
    private static final Logger LOG = LoggerFactory.getLogger(StreamObserverFluxAdapter.class);

    private final Flux<T> flux;
    private FluxSink<T> sink;
    private BlockingQueue<T> buffer;
    private final AtomicBoolean buffering = new AtomicBoolean(true);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private volatile Throwable earlyError = null;

    public StreamObserverFluxAdapter() {
        this.flux = Flux.create(sink -> {
            LOG.trace("Flux.create()");
            this.sink = sink;
            if (earlyError != null) {
                sink.error(earlyError);
            }
            if (buffer != null && !buffer.isEmpty()) {
                LOG.trace("buffer not empty: size: {}", buffer.size());
                while (!buffer.isEmpty()) {
                    publish(buffer.poll());
                }
                if (completed.get()) {
                    sink.complete();
                }
            }
            buffer = null;
            buffering.set(false);
        });
    }

    private void publish(T next) {
        sink.next(next);
    }

    public Flux<T> toFlux() {
        return flux;
    }

    @Override
    public void onNext(T t) {
        if (buffering.get()) {
            buffer(t);
        } else {
            publish(t);
        }
    }

    private void buffer(T t) {
        if (buffer == null) {
            buffer = new ArrayBlockingQueue<>(BUFFER_SIZE);
        }
        try {
            LOG.trace("buffering size {}", buffer.size());
            buffer.put(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (sink != null) {
            sink.error(throwable);
        } else {
            earlyError = throwable;
        }
    }

    @Override
    public void onCompleted() {
        if (sink != null) {
            sink.complete();
        } else {
            completed.set(true);
        }
    }
}
