package io.mubel.sdk.subscription;

import io.mubel.api.grpc.EventData;
import io.mubel.api.grpc.SubscribeRequest;
import io.mubel.client.MubelClient;
import io.mubel.client.Subscription;
import io.mubel.sdk.TestComponents;
import io.mubel.sdk.fixtures.Fixtures;
import io.mubel.sdk.fixtures.TestEvents;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubscriptionWorkerTest {

    final String consumerGroup = "consumerGroup";

    SubscriptionStateRepository repository = mock(SubscriptionStateRepository.class);

    MubelClient factory = mock(MubelClient.class);

    BlockingQueue<EventData> buffer = new ArrayBlockingQueue<>(100);

    TestEventConsumer<TestEvents> eventConsumer = TestComponents.testEventConsumer();

    SubscriptionWorker worker = SubscriptionWorker.builder()
            .client(factory)
            .stateRepository(repository)
            .eventDataMapper(TestComponents.eventDataMapper())
            .build();

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    Future<?> workerFuture;

    static {
        Awaitility.setDefaultTimeout(Duration.ofSeconds(60));
    }

    @BeforeEach
    void setup() {
        Awaitility.setDefaultTimeout(Duration.ofSeconds(1));
        when(factory.subscribe(any(SubscribeRequest.class), anyInt()))
                .thenReturn(new Subscription() {
                    @Override
                    public EventData next() throws InterruptedException {
                        return buffer.take();
                    }

                    @Override
                    public List<EventData> nextBatch(int size) throws InterruptedException {
                        final var batch = new ArrayList<EventData>(size);
                        buffer.drainTo(batch, size);
                        if (batch.isEmpty()) {
                            batch.add(buffer.take());
                            buffer.drainTo(batch, size - 1);
                        }
                        return batch;
                    }
                });
    }

    @AfterEach
    void teardown() {
        worker.stop();
        executorService.shutdownNow();
        assertThatThrownBy(() -> workerFuture.get(1, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(InterruptedException.class);
    }

    @Test
    void baseCase() {
        setupWithNoPreexistingState();
        startWorker();
        assertThat(eventConsumer.getEvents()).isEmpty();
        var ed = createEventData();
        send(ed);
        await().untilAsserted(() -> assertThat(eventConsumer.getEvents()).hasSize(1));
        verify(repository).put(expectedState(ed, 0));
    }

    private SubscriptionState expectedState(EventData ed, int version) {
        return new SubscriptionState(consumerGroup, ed.getSequenceNo(), version);
    }

    private void send(EventData... eventData) {
        Collections.addAll(buffer, eventData);
    }

    private EventData createEventData() {
        return Fixtures.eventDataBuilder().build();
    }

    private void startWorker() {
        var params = SubscriptionConfig.<TestEvents>builder()
                .consumer(eventConsumer)
                .eventBaseClass(TestEvents.class)
                .consumerGroup(consumerGroup)
                .eventStoreId(Fixtures.esid())
                .batchSize(100)
                .build();

        workerFuture = executorService.submit(() -> {
            try {
                worker.start(params);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }

    void setupWithNoPreexistingState() {
        when(repository.find(consumerGroup)).thenReturn(Optional.empty());
    }
}