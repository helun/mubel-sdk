package io.mubel.sdk.scheduled;

import io.mubel.api.grpc.ScheduledEvent;
import io.mubel.api.grpc.ScheduledEventsSubscribeRequest;
import io.mubel.api.grpc.TriggeredEvents;
import io.mubel.client.MubelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class ScheduledEventsSubscriptionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledEventsSubscriptionFactory.class);

    private final MubelClient client;
    private final Executor executor;

    public static Builder builder() {
        return new Builder();
    }

    public ScheduledEventsSubscriptionFactory(MubelClient client, Executor executor) {
        this.client = client;
        this.executor = executor;
    }

    public <T> ScheduledEventsSubscription create(ScheduledEventsConfig<T> params) {
        final var buffer = new ArrayBlockingQueue<ScheduledEvent>(100);
        start(buffer, params);
        return new ScheduledEventsSubscription(buffer);
    }

    private <T> void start(BlockingQueue<ScheduledEvent> buffer, ScheduledEventsConfig<T> params) {
        executor.execute(() -> {
            LOG.info("Scheduled events subscription starting: {}", getCategoriesString(params.categories()));
            try {
                final var stream = client.subscribeToScheduledEvents(ScheduledEventsSubscribeRequest.newBuilder()
                        .addAllCategory(params.categories())
                        .build());
                while (stream.hasNext()) {
                    final var e = stream.next();
                    offer(buffer, e);
                }
                LOG.info("Scheduled events subscription stream stopped");
            } catch (InterruptedException e) {
                LOG.error("Scheduled events subscription stream was interrupted");
                Thread.currentThread().interrupt();
            }
        });
    }

    private static void offer(BlockingQueue<ScheduledEvent> buffer, TriggeredEvents e) throws InterruptedException {
        for (var event : e.getEventList()) {
            while (!buffer.offer(event, 1, TimeUnit.MINUTES)) {
                LOG.warn("Buffer is full, retrying");
            }
        }
    }

    private String getCategoriesString(Set<String> categories) {
        return categories.isEmpty() ? "all" : String.join(", ", categories);
    }

    public static class Builder {
        private MubelClient client;
        private Executor executor;

        public Builder client(MubelClient client) {
            this.client = client;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public ScheduledEventsSubscriptionFactory build() {
            return new ScheduledEventsSubscriptionFactory(client, executor);
        }
    }
}
