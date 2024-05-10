package io.mubel.sdk.subscription;

import io.mubel.api.grpc.EventData;
import io.mubel.api.grpc.JoinConsumerGroupRequest;
import io.mubel.api.grpc.SubscribeRequest;
import io.mubel.client.MubelClient;
import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.EventMessage;
import io.mubel.sdk.EventMessageBatch;
import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;
import io.mubel.sdk.tx.TransactionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.mubel.sdk.internal.reflection.TypeChecker.ensureAssignable;

public class SubscriptionWorker {

    private final static Logger LOG = LoggerFactory.getLogger(SubscriptionWorker.class);
    private final MubelClient client;
    private final SubscriptionStateRepository stateRepository;
    private final TransactionAdapter transactionAdapter;
    private final EventDataMapper mapper;

    private final AtomicBoolean shouldRun = new AtomicBoolean(true);
    private Disposable subDisposable;

    public static Builder builder() {
        return new Builder();
    }

    private SubscriptionWorker(
            Builder b) {
        this.client = b.client;
        this.stateRepository = b.stateRepository;
        this.transactionAdapter = b.transactionAdapter;
        this.mapper = b.eventDataMapper;
    }

    public <T> void start(SubscriptionConfig<T> config) throws InterruptedException {
        waitForGroupLeadership(config);
        final var state = getSubscriptionState(config);
        final var stateRef = new AtomicReference<>(state);
        final var consumer = config.consumer();
        this.subDisposable = start(config, state.sequenceNumber())
                .bufferTimeout(config.batchSize(), Duration.ofMillis(250))
                .doOnSubscribe(sub -> LOG.info("Subscription worker: consumer group: {}, started from sequence number: {}", config.consumerGroup(), state.sequenceNumber()))
                .doOnError(e -> LOG.error("Subscription worker: consumer group: {}, error", config.consumerGroup(), e))
                .subscribe(batch -> {
                    LOG.debug("Subscription worker: consumer group: {}, received batch of {} events", config.consumerGroup(), batch.size());
                    final var mappedMessages = map(config, batch);
                    transactionAdapter.execute(() -> {
                        consumer.accept(mappedMessages);
                        updateSubscriptionState(stateRef, mappedMessages);
                    });
                });
    }

    private <T> void waitForGroupLeadership(SubscriptionConfig<T> config) throws InterruptedException {
        try {
            var joinFuture = client.joinConsumerGroup(JoinConsumerGroupRequest.newBuilder()
                    .setConsumerGroup(config.consumerGroup())
                    .setEsid(config.eventStoreId())
                    .build());
            LOG.info("Subscription worker: consumer group: {}, joining consumer group", config.consumerGroup());
            var leaderStatus = joinFuture.get();
            if (leaderStatus.getLeader()) {
                LOG.info("Subscription worker: consumer group: {}, joined consumer group as leader", config.consumerGroup());
            } else {
                throw new IllegalStateException("Consumer group leader is not this instance");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private static int calculateBufferSize(SubscriptionConfig<?> params) {
        return params.batchSize() * 2;
    }

    private Flux<EventData> start(SubscriptionConfig<?> params, long fromSequenceNo) {
        final var request = SubscribeRequest.newBuilder()
                .setEsid(params.eventStoreId())
                .setFromSequenceNo(fromSequenceNo)
                .build();
        return client.subscribe(request, calculateBufferSize(params));
    }

    private <T> SubscriptionState getSubscriptionState(SubscriptionConfig<T> config) {
        return stateRepository.find(config.consumerGroup())
                .orElseGet(() -> SubscriptionState.initialState(config.consumerGroup()));
    }

    private <T> void updateSubscriptionState(AtomicReference<SubscriptionState> stateRef, EventMessageBatch<T> mappedMessages) {
        final var state = stateRef.get().withSequenceNumber(mappedMessages.lastSequenceNo());
        stateRepository.put(state);
        stateRef.set(state);
    }

    public void stop() {
        if (subDisposable != null) {
            subDisposable.dispose();
        }
    }

    private <T> EventMessageBatch<T> map(SubscriptionConfig<T> config, List<EventData> input) {
        final var messages = new ArrayList<EventMessage<T>>(input.size());
        for (var eventData : input) {
            messages.add(new EventMessage<>(ensureAssignable(mapper.fromEventData(eventData), config.eventBaseClass()), eventData));
        }
        return new EventMessageBatch<>(messages, input.getLast().getSequenceNo());
    }

    public static class Builder {
        private MubelClient client;
        private EventDataMapper eventDataMapper;
        private SubscriptionStateRepository stateRepository;
        private TransactionAdapter transactionAdapter;

        public Builder client(MubelClient client) {
            this.client = client;
            return this;
        }

        /**
         * @param eventDataMapper
         * @return this
         */
        public Builder eventDataMapper(EventDataMapper eventDataMapper) {
            this.eventDataMapper = eventDataMapper;
            return this;
        }

        /**
         * @param stateRepository
         * @return this
         */
        public Builder stateRepository(SubscriptionStateRepository stateRepository) {
            this.stateRepository = stateRepository;
            return this;
        }

        /**
         * Optional. If not provided, a no-op transaction adapter will be used.
         *
         * @param transactionAdapter
         * @return this
         */
        public Builder transactionAdapter(TransactionAdapter transactionAdapter) {
            this.transactionAdapter = transactionAdapter;
            return this;
        }

        public SubscriptionWorker build() {
            Utils.requireNonNull(client, () -> new MubelConfigurationException("client may not be null"));
            Utils.requireNonNull(eventDataMapper, () -> new MubelConfigurationException("eventDataMapper may not be null"));
            Utils.requireNonNull(stateRepository, () -> new MubelConfigurationException("stateRepository may not be null"));
            transactionAdapter = Objects.requireNonNullElseGet(transactionAdapter, TransactionAdapter::noOpTransactionAdapter);
            return new SubscriptionWorker(this);
        }
    }

}
