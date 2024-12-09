package io.mubel.sdk.scheduled;

import io.mubel.api.grpc.v1.events.Deadline;
import io.mubel.api.grpc.v1.events.DeadlineSubscribeRequest;
import io.mubel.client.MubelClient;
import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Constants;
import io.mubel.sdk.internal.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class ExpiredDeadlineHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ExpiredDeadlineHandler.class);
    private final String esid;
    private final List<ExpiredDeadlineConsumer> consumers;
    private final MubelClient client;
    private final EventDataMapper eventDataMapper;
    private final Clock clock;
    private final Duration longPolllTimeout;

    private ExpiredDeadlineHandler(Builder b) {
        this.esid = b.esid;
        this.consumers = b.consumers;
        this.client = b.client;
        this.eventDataMapper = b.eventDataMapper;
        this.clock = b.clock;
        this.longPolllTimeout = b.longPolllTimeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void start() {
        if (consumers.isEmpty()) {
            return;
        }
        final var subscription = client.subscribeToDeadlines(DeadlineSubscribeRequest.newBuilder()
                        .setEsid(esid)
                        .setTimeout((int) longPolllTimeout.toSeconds())
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
        private String esid;
        private List<ExpiredDeadlineConsumer> consumers;
        private MubelClient client;
        private EventDataMapper eventDataMapper;
        private Clock clock;
        private Duration longPolllTimeout = Duration.ofSeconds(20);

        public Builder esid(String esid) {
            this.esid = esid;
            return this;
        }

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

        public Builder longPolllTimeout(Duration longPolllTimeout) {
            this.longPolllTimeout = longPolllTimeout;
            return this;
        }

        public ExpiredDeadlineHandler build() {
            clock = Objects.requireNonNullElseGet(clock, Clock::systemUTC);
            Utils.requireNonNull(esid, () -> new MubelConfigurationException("esid (Event Store id)  may not be null"));
            Utils.requireNonNull(consumers, () -> new MubelConfigurationException("Consumers may not be null"));
            Utils.requireNonNull(client, () -> new MubelConfigurationException("Client may not be null"));
            Utils.requireNonNull(eventDataMapper, () -> new MubelConfigurationException("Event data mapper may not be null"));
            Utils.requireNonNull(clock, () -> new MubelConfigurationException("Clock may not be null"));
            Utils.requireNonNull(longPolllTimeout, () -> new MubelConfigurationException("longPollTimeout may not be null"));
            Utils.assertMaxValue(
                    longPolllTimeout.toSeconds(),
                    Constants.LONG_POLL_MAX_VALUE,
                    (actual) -> new MubelConfigurationException("longPollTimeout may not be greater that %d. was %d".formatted(Constants.LONG_POLL_MAX_VALUE, actual))
            );
            return new ExpiredDeadlineHandler(this);
        }
    }
}
