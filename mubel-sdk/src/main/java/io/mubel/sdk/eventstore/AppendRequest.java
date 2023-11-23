package io.mubel.sdk.eventstore;

import io.mubel.api.grpc.EventDataInput;
import io.mubel.api.grpc.ScheduledEvent;

import java.util.List;
import java.util.Set;

/**
 * Represents a request to append events to the event store.
 *
 * @param events          The events to append to the event store.
 * @param scheduledEvents Optional list of events or deadlines to schedule for publication at a later time.
 * @param cancelIds       Optional set of ids of scheduled events or deadlines to cancel.
 */
public record AppendRequest(
        List<EventDataInput> events,
        List<ScheduledEvent> scheduledEvents,
        Set<String> cancelIds
) {
}
