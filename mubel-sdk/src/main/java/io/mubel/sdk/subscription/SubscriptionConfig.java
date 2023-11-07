package io.mubel.sdk.subscription;

import io.mubel.sdk.Constrains;
import io.mubel.sdk.EventMessageBatchConsumer;
import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;

/**
 * Configuration for a subscription.
 *
 * @param consumer      consumer to process events with
 * @param eventStoreId  event store to subscribe to
 * @param consumerGroup consumer group to subscribe with
 * @param batchSize     number of events to process in one transaction
 */
public record SubscriptionConfig<T>(
        EventMessageBatchConsumer<T> consumer,
        Class<T> eventBaseClass,
        String eventStoreId,
        String consumerGroup,
        int batchSize
) {
    @SuppressWarnings("ConstantConditions")
    public SubscriptionConfig {
        consumer = Utils.requireNonNull(consumer, () -> new MubelConfigurationException("consumer may not be null"));
        eventBaseClass = Utils.requireNonNull(eventBaseClass, () -> new MubelConfigurationException("eventBaseClass may not be null"));
        eventStoreId = Constrains.validateEventStoreId(eventStoreId);
        consumerGroup = Utils.requireNonNull(consumerGroup, () -> new MubelConfigurationException("consumerGroup may not be null"));
        batchSize = Utils.assertPositive(batchSize, value -> new MubelConfigurationException("batchSize must be > 0. was: %d".formatted(value)));
    }

    @Override
    public String toString() {
        return "SubscriptionConfig{" +
                "eventBaseClass=" + eventBaseClass +
                ", eventStoreId='" + eventStoreId +
                ", consumerGroup='" + consumerGroup +
                ", batchSize=" + batchSize +
                "'}'";
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private EventMessageBatchConsumer<T> consumer;
        private Class<T> eventBaseClass;
        private String eventStoreId;
        private String consumerGroup;
        private int batchSize;

        public Builder<T> consumer(EventMessageBatchConsumer<T> consumer) {
            this.consumer = consumer;
            return this;
        }

        public Builder<T> eventBaseClass(Class<T> eventBaseClass) {
            this.eventBaseClass = eventBaseClass;
            return this;
        }

        public Builder<T> eventStoreId(String eventStoreId) {
            this.eventStoreId = eventStoreId;
            return this;
        }

        public Builder<T> consumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
            return this;
        }

        public Builder<T> batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public SubscriptionConfig<T> build() {
            return new SubscriptionConfig<>(
                    consumer,
                    eventBaseClass,
                    eventStoreId,
                    consumerGroup,
                    batchSize
            );
        }
    }
}
