package io.mubel.sdk.scheduled;

import io.mubel.api.grpc.ScheduledEventsSubscribeRequest;
import io.mubel.api.grpc.TriggeredEvents;
import io.mubel.client.MubelClient;
import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExpiredDeadlineHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ExpiredDeadlineHandler.class);
    private final List<ExpiredDeadlineConsumer> consumers;
    private final Executor executor;
    private final MubelClient client;
    private final EventDataMapper eventDataMapper;
    private final Clock clock;
    private final AtomicBoolean shouldRun = new AtomicBoolean(true);

    private ExpiredDeadlineHandler(Builder b) {
        this.consumers = b.consumers;
        this.executor = b.executor;
        this.client = b.client;
        this.eventDataMapper = b.eventDataMapper;
        this.clock = b.clock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void start() {
        executor.execute(() -> {
            LOG.info("Expired deadline handler starting");
            final var subscription = client.subscribeToScheduledEvents(ScheduledEventsSubscribeRequest.newBuilder()
                            .build(),
                    100
            );
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

    public void accept(TriggeredEvents events) {
        for (final var event : events.getEventList()) {
            LOG.debug("Received expired deadline event: {}", event.getId());
            final var expired = eventDataMapper.mapExpiredDeadline(event, clock.instant());
            for (final var consumer : consumers) {
                if (consumer.targetType().equals(event.getTargetType())) {
                    executor.execute(() -> consumer.deadlineExpired(expired));
                    break;
                }
            }
        }
    }

    public static class Builder {
        private List<ExpiredDeadlineConsumer> consumers;
        private Executor executor;
        private MubelClient client;
        private EventDataMapper eventDataMapper;
        private Clock clock;

        public Builder consumers(List<ExpiredDeadlineConsumer> consumers) {
            this.consumers = consumers;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder client(MubelClient subscriptionFactory) {
            this.client = subscriptionFactory;
            return this;
        }

        public Builder eventDataMapper(EventDataMapper eventDataMapper) {
            this.eventDataMapper = eventDataMapper;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public ExpiredDeadlineHandler build() {
            clock = Objects.requireNonNullElseGet(clock, Clock::systemUTC);
            Utils.requireNonNull(consumers, () -> new MubelConfigurationException("Consumers may not be null"));
            Utils.requireNonNull(executor, () -> new MubelConfigurationException("Executor may not be null"));
            Utils.requireNonNull(client, () -> new MubelConfigurationException("Client may not be null"));
            Utils.requireNonNull(eventDataMapper, () -> new MubelConfigurationException("Event data mapper may not be null"));
            return new ExpiredDeadlineHandler(this);
        }
    }
}
