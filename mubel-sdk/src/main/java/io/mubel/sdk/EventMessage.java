package io.mubel.sdk;

import com.google.common.base.Objects;
import io.mubel.api.grpc.EventData;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

/**
 * Represents an event with a decoded body.
 *
 * @param <T>
 */
public class EventMessage<T> {

    private final T body;
    private final EventData eventData;

    public EventMessage(T body, EventData eventData) {
        this.body = requireNonNull(body, "body may not be null");
        this.eventData = requireNonNull(eventData, "eventData may not be null");
    }

    /**
     * @return the decoded body of the event
     */
    public T body() {
        return body;
    }

    /**
     * @return the event id
     */
    public String eventId() {
        return eventData.getId();
    }

    /**
     * @return the stream id
     */
    public String streamId() {
        return eventData.getStreamId();
    }

    /**
     * @return the event type
     */
    public String type() {
        return eventData.getType();
    }

    /**
     * TODO: clairfy the difference between sequenceNo and version
     *
     * @return the sequence number if this event in the stream
     */
    public long sequenceNo() {
        return eventData.getSequenceNo();
    }

    /**
     * @return the version of this event for this stream id
     */
    public int version() {
        return eventData.getVersion();
    }

    /**
     * @return the timestamp of this event
     */
    public Instant timestamp() {
        return Instant.ofEpochMilli(eventData.getCreatedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(eventData.getId());
    }

    @Override
    public boolean equals(Object other) {
        return Objects.equal(this, other);
    }
}
