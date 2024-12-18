package io.mubel.sdk.subscription;

import io.mubel.api.grpc.v1.events.EventData;
import io.mubel.api.grpc.v1.events.SubscribeRequest;
import io.mubel.api.grpc.v1.groups.GroupStatus;
import io.mubel.client.MubelClient;
import io.mubel.sdk.TestComponents;
import io.mubel.sdk.fixtures.Fixtures;
import io.mubel.sdk.fixtures.TestEvents;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionWorkerTest {

    static final Logger LOG = LoggerFactory.getLogger(SubscriptionWorkerTest.class);

    final String consumerGroup = "consumerGroup";

    SubscriptionStateRepository repository = mock(SubscriptionStateRepository.class);

    MubelClient client = mock(MubelClient.class);

    TestEventConsumer<TestEvents> eventConsumer = TestComponents.testEventConsumer();

    SubscriptionWorker worker = SubscriptionWorker.builder()
            .client(client)
            .stateRepository(repository)
            .eventDataMapper(TestComponents.eventDataMapper())
            .build();

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    Future<?> workerFuture;

    FluxSink<EventData> subscriptionSink;
    CountDownLatch latch = new CountDownLatch(1);

    SubscriptionConfig.Builder<TestEvents> configBuilder = SubscriptionConfig.<TestEvents>builder()
            .consumer(eventConsumer)
            .eventBaseClass(TestEvents.class)
            .eventStoreId("es:id")
            .consumerGroup(consumerGroup)
            .batchSize(10);

    static {
        Awaitility.setDefaultTimeout(Duration.ofSeconds(60));
    }

    @BeforeEach
    void setup() {
        LOG.info("setup");
        Awaitility.setDefaultTimeout(Duration.ofSeconds(1));
        Flux<EventData> subscription = Flux.push(sink -> {
            subscriptionSink = requireNonNull(sink);
            latch.countDown();
        });
        when(client.subscribe(any(SubscribeRequest.class)))
                .thenReturn(subscription);
    }

    @AfterEach
    void teardown() {
        LOG.info("tearDown");
        worker.stop();
        executorService.shutdownNow();
    }

    @Test
    void Subscribe_base_case() {
        LOG.info("Subscribe_base_case");
        setupWithNoPreexistingState();
        setupGroupLeader().tryEmitNext(consumerGroupStatus(true));
        startWorker();
        assertThat(eventConsumer.getEvents()).isEmpty();
        var ed = createEventData();
        send(ed);
        await().untilAsserted(() -> assertThat(eventConsumer.getEvents()).hasSize(1));
        verify(repository).put(expectedState(ed, 0));
        verify(client).joinConsumerGroup(any());
    }

    @Test
    void Waits_for_group_leadership_before_subscribing() {
        setupWithNoPreexistingState();
        var leaderSink = setupGroupLeader();
        startWorker();
        await().untilAsserted(() -> verify(client).joinConsumerGroup(any()));
        verify(client, never()).subscribe(any());
        leaderSink.tryEmitNext(consumerGroupStatus(true));
        await().untilAsserted(() -> verify(client).subscribe(any()));
    }

    private Sinks.Many<GroupStatus> setupGroupLeader() {
        Sinks.Many<GroupStatus> sink = Sinks.many()
                .unicast()
                .onBackpressureBuffer();

        when(client.joinConsumerGroup(any())).thenReturn(sink.asFlux());
        return sink;
    }

    private GroupStatus consumerGroupStatus(boolean leader) {
        return GroupStatus.newBuilder()
                .setGroupId(consumerGroup)
                .setLeader(leader)
                .build();
    }

    private SubscriptionState expectedState(EventData ed, int version) {
        return new SubscriptionState(consumerGroup, ed.getSequenceNo(), version);
    }

    private void send(EventData... eventData) {
        LOG.info("send sink {}", subscriptionSink);
        try {
            latch.await();
            for (var ed : eventData) {
                subscriptionSink.next(ed);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private EventData createEventData() {
        return Fixtures.eventDataBuilder().build();
    }


    private void startWorker() {
        startWorker(configBuilder.build());
    }

    private void startWorker(SubscriptionConfig<TestEvents> params) {
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