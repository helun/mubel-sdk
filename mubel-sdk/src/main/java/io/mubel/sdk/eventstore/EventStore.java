package io.mubel.sdk.eventstore;


import io.mubel.api.grpc.EventData;
import io.mubel.api.grpc.EventDataInput;

import java.util.List;

public interface EventStore {
    /**
     * Append events to the event store.
     *
     * @param events The events to append.
     */
    void append(List<EventDataInput> events);

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
