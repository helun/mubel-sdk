package io.mubel.sdk.eventstore;


import io.mubel.api.grpc.v1.events.EventData;
import io.mubel.api.grpc.v1.events.ExecuteRequestOrBuilder;

import java.util.List;

public interface EventStore {

    /**
     * Append events to the event store.
     * Any scheduled events or deadlines will be registered for later publishing.
     * Any cancel ids will be used to cancel any scheduled events or deadlines.
     *
     * @param appendRequest
     */
    void execute(ExecuteRequestOrBuilder appendRequest);

    /**
     * @param streamId The stream id of the aggregate to get events for.
     * @return The events for the aggregate with the given stream id.
     */
    List<EventData> get(String streamId);

    /**
     * @param streamId The stream id of the aggregate to get events for.
     * @param version  The version of the aggregate to get events for.
     * @return The events for the aggregate with the given stream id and version.
     */
    List<EventData> get(String streamId, int version);
}
