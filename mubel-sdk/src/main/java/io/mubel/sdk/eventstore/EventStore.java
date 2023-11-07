package io.mubel.sdk.eventstore;


import io.mubel.api.grpc.EventData;
import io.mubel.api.grpc.EventDataInput;

import java.util.List;

public interface EventStore {

    void append(List<EventDataInput> events);

    List<EventData> get(String streamId);

}
