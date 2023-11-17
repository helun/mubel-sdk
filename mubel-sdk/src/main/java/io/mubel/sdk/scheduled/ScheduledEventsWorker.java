package io.mubel.sdk.scheduled;

import io.mubel.api.grpc.ScheduledEvent;
import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.subscription.SubscriptionConfig;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.mubel.sdk.internal.reflection.TypeChecker.ensureAssignable;

public class ScheduledEventsWorker {

    private final EventDataMapper mapper;
    private final ScheduledEventsSubscriptionFactory subscriptionFactory;
    private final AtomicBoolean shouldRun = new AtomicBoolean(true);

    public ScheduledEventsWorker(EventDataMapper mapper, ScheduledEventsSubscriptionFactory subscriptionFactory) {
        this.mapper = mapper;
        this.subscriptionFactory = subscriptionFactory;
    }

    public <T> void start(ScheduledEventsConfig<T> config) throws InterruptedException {
        final var subscription = subscriptionFactory.create(config);
        try {
            final var consumer = config.consumer();
            while (shouldRun.get()) {
                subscription.next();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private <T> T map(SubscriptionConfig<T> config, ScheduledEvent input) {
        return ensureAssignable(mapper.fromScheduledEvent(input), config.eventBaseClass());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EventDataMapper mapper;
        private ScheduledEventsSubscriptionFactory subscriptionFactory;

        public Builder mapper(EventDataMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder subscriptionFactory(ScheduledEventsSubscriptionFactory subscriptionFactory) {
            this.subscriptionFactory = subscriptionFactory;
            return this;
        }

        public ScheduledEventsWorker build() {
            return new ScheduledEventsWorker(mapper, subscriptionFactory);
        }
    }

}
