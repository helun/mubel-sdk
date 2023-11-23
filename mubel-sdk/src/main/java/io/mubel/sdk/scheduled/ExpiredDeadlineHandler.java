package io.mubel.sdk.scheduled;

import io.mubel.api.grpc.ScheduledEvent;
import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Constants;
import io.mubel.sdk.internal.Utils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExpiredDeadlineHandler {
    private final List<ExpiredDeadlineConsumer> consumers;
    private final Executor executor;
    private final ScheduledEventsSubscriptionFactory subscriptionFactory;
    private final AtomicBoolean shouldRun = new AtomicBoolean(true);

    private ExpiredDeadlineHandler(Builder b) {
        this.consumers = b.consumers;
        this.executor = b.executor;
        this.subscriptionFactory = b.subscriptionFactory;
    }

    public void start() {
        executor.execute(() -> {
            final var subscription = subscriptionFactory.create(ScheduledEventsConfig.forCategories(Constants.DEADLINE_CATEGORY_NAME));
            try {
                while (shouldRun.get()) {
                    accept(subscription.next());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }

    public void stop() {
        shouldRun.set(false);
    }

    public void accept(ScheduledEvent event) {
        final var expired = mapExpiredDeadline(event);
        for (final var consumer : consumers) {
            if (consumer.targetType().equals(event.getTargetType())) {
                executor.execute(() -> consumer.accept(expired));
                break;
            }
        }
    }

    private static ExpiredDeadline mapExpiredDeadline(ScheduledEvent event) {
        return new ExpiredDeadline(
                UUID.fromString(event.getTargetEntityId()),
                event.getMetaData().getDataOrDefault(Constants.DEADLINE_NAME_METADATA_KEY, "")
        );
    }

    public static class Builder {
        private List<ExpiredDeadlineConsumer> consumers;
        private Executor executor;
        private ScheduledEventsSubscriptionFactory subscriptionFactory;

        public Builder consumers(List<ExpiredDeadlineConsumer> consumers) {
            this.consumers = consumers;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder subscriptionFactory(ScheduledEventsSubscriptionFactory subscriptionFactory) {
            this.subscriptionFactory = subscriptionFactory;
            return this;
        }

        public ExpiredDeadlineHandler build() {
            Utils.requireNonNull(consumers, () -> new MubelConfigurationException("Consumers may not be null"));
            Utils.requireNonNull(executor, () -> new MubelConfigurationException("Executor may not be null"));
            Utils.requireNonNull(subscriptionFactory, () -> new MubelConfigurationException("Subscription factory may not be null"));
            return new ExpiredDeadlineHandler(this);
        }
    }
}
