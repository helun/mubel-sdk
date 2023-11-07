package io.mubel.sdk.subscription;

import io.mubel.api.grpc.EventData;
import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.EventMessage;
import io.mubel.sdk.EventMessageBatch;
import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;
import io.mubel.sdk.tx.TransactionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.mubel.sdk.internal.reflection.TypeChecker.ensureAssignable;

public class SubscriptionWorker {

    private final static Logger LOG = LoggerFactory.getLogger(SubscriptionWorker.class);
    private final SubscriptionFactory subscriptionFactory;
    private final SubscriptionStateRepository stateRepository;
    private final TransactionAdapter transactionAdapter;
    private final EventDataMapper mapper;

    private final AtomicBoolean shouldRun = new AtomicBoolean(true);

    public static Builder builder() {
        return new Builder();
    }

    private SubscriptionWorker(
            Builder b) {
        this.subscriptionFactory = b.subscriptionFactory;
        this.stateRepository = b.stateRepository;
        this.transactionAdapter = b.transactionAdapter;
        this.mapper = b.eventDataMapper;
    }

    public <T> void start(SubscriptionConfig<T> config) throws InterruptedException {
        final var state = getSubscriptionState(config);
        final var subscription = subscriptionFactory.create(config, state.sequenceNumber());
        try {
            LOG.info("Subscription worker: consumer group: {}, started from sequence number: {}", config.consumerGroup(), state.sequenceNumber());
            final var stateRef = new AtomicReference<>(state);
            final var consumer = config.consumer();
            while (shouldRun.get()) {
                final var batch = subscription.nextBatch(config.batchSize());
                final var mappedMessages = map(config, batch);
                transactionAdapter.execute(() -> {
                    consumer.accept(mappedMessages);
                    updateSubscriptionState(stateRef, mappedMessages);
                });
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            LOG.error("Subscription worker: consumer group: {}, failed", config.consumerGroup(), e);
            throw e;
        }
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
        shouldRun.set(false);
    }

    private <T> EventMessageBatch<T> map(SubscriptionConfig<T> config, List<EventData> input) {
        final var messages = new ArrayList<EventMessage<T>>(input.size());
        for (var eventData : input) {
            messages.add(new EventMessage<>(ensureAssignable(mapper.fromEventData(eventData), config.eventBaseClass()), eventData));
        }
        return new EventMessageBatch<>(messages, input.getLast().getSequenceNo());
    }

    public static class Builder {
        private EventDataMapper eventDataMapper;
        private SubscriptionFactory subscriptionFactory;
        private SubscriptionStateRepository stateRepository;
        private TransactionAdapter transactionAdapter;

        /**
         * @param eventDataMapper
         * @return this
         */
        public Builder eventDataMapper(EventDataMapper eventDataMapper) {
            this.eventDataMapper = eventDataMapper;
            return this;
        }

        /**
         * @param subscriptionFactory
         * @return this
         */
        public Builder subscriptionFactory(SubscriptionFactory subscriptionFactory) {
            this.subscriptionFactory = subscriptionFactory;
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
            Utils.requireNonNull(eventDataMapper, () -> new MubelConfigurationException("eventDataMapper may not be null"));
            Utils.requireNonNull(subscriptionFactory, () -> new MubelConfigurationException("eventSubscription may not be null"));
            Utils.requireNonNull(stateRepository, () -> new MubelConfigurationException("stateRepository may not be null"));
            transactionAdapter = Objects.requireNonNullElseGet(transactionAdapter, TransactionAdapter::noOpTransactionAdapter);
            return new SubscriptionWorker(this);
        }
    }

}
