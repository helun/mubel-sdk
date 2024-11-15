package io.mubel.sdk.scheduled;

import io.mubel.api.grpc.v1.events.Deadline;
import io.mubel.api.grpc.v1.events.DeadlineSubscribeRequest;
import io.mubel.client.MubelClient;
import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public class ExpiredDeadlineHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ExpiredDeadlineHandler.class);
    private final List<ExpiredDeadlineConsumer> consumers;
    private final MubelClient client;
    private final EventDataMapper eventDataMapper;
    private final Clock clock;

    private ExpiredDeadlineHandler(Builder b) {
        this.consumers = b.consumers;
        this.client = b.client;
        this.eventDataMapper = b.eventDataMapper;
        this.clock = b.clock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void start() {
        final var subscription = client.subscribeToDeadlines(DeadlineSubscribeRequest.newBuilder()
                        .setEsid("todo")
                        .setTimeout(20)
                        .build())
                .repeat()
                .subscribe(this::accept);
    }

    public void stop() {

    }

    public void accept(Deadline deadline) {
        final var expired = eventDataMapper.mapExpiredDeadline(deadline, clock.instant());
        for (final var consumer : consumers) {
            if (consumer.targetType().equals(deadline.getTargetEntity().getType())) {
                consumer.deadlineExpired(expired);
                break;
            }
        }
    }

    public static class Builder {
        private List<ExpiredDeadlineConsumer> consumers;
        private MubelClient client;
        private EventDataMapper eventDataMapper;
        private Clock clock;

        public Builder consumers(List<ExpiredDeadlineConsumer> consumers) {
            this.consumers = consumers;
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
            Utils.requireNonNull(client, () -> new MubelConfigurationException("Client may not be null"));
            Utils.requireNonNull(eventDataMapper, () -> new MubelConfigurationException("Event data mapper may not be null"));
            return new ExpiredDeadlineHandler(this);
        }
    }
}
