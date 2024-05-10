package io.mubel.client.internal;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StreamObserverFluxAdapterTest {

    @Test
    void normal_case() throws Exception {
        StreamObserverFluxAdapter<Integer> adapter = new StreamObserverFluxAdapter<>();
        AtomicInteger sum = new AtomicInteger();
        var subSubscribed = new CountDownLatch(1);
        var subCompleted = new CountDownLatch(1);
        adapter.toFlux()
                .subscribeOn(Schedulers.single())
                .doOnSubscribe(s -> {
                    Flux.just(1, 2, 3)
                            .delaySubscription(Duration.ofMillis(100))
                            .subscribeOn(Schedulers.single())
                            .subscribe(adapter::onNext, adapter::onError, adapter::onCompleted);
                })
                .doOnComplete(subCompleted::countDown)
                .subscribe(sum::addAndGet);

        subCompleted.await(100, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertThat(sum).hasValue(6);
    }

    @Test
    void buffer_when_not_subscribed_to() {
        StreamObserverFluxAdapter<Integer> adapter = new StreamObserverFluxAdapter<>();
        Flux<Integer> flux = adapter.toFlux();
        adapter.onNext(1);
        adapter.onNext(2);
        adapter.onNext(3);
        adapter.onCompleted();
        assertThat(flux.collectList().block()).containsExactly(1, 2, 3);
    }

    @Test
    void emits_early_error_when_subscribed_to() {
        StreamObserverFluxAdapter<Integer> adapter = new StreamObserverFluxAdapter<>();
        Flux<Integer> flux = adapter.toFlux();
        adapter.onError(new RuntimeException("test"));
        assertThatThrownBy(flux::blockLast).hasMessage("test");
    }

}