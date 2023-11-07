package io.mubel.sdk.subscription;

import io.mubel.api.grpc.EventData;
import io.mubel.sdk.TestComponents;
import io.mubel.sdk.fixtures.Fixtures;
import io.mubel.sdk.fixtures.TestEvents;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class SubscriptionWorkerTest {

    final String consumerGroup = "consumerGroup";

    SubscriptionStateRepository repository = mock(SubscriptionStateRepository.class);

    SubscriptionFactory factory = mock(SubscriptionFactory.class);

    BlockingQueue<EventData> buffer = new ArrayBlockingQueue<>(100);

    TestEventConsumer<TestEvents> eventConsumer = TestComponents.testEventConsumer();

    SubscriptionWorker worker = SubscriptionWorker.builder()
            .subscriptionFactory(factory)
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
        when(factory.create(any(), anyLong()))
                .thenReturn(new Subscription(buffer));
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