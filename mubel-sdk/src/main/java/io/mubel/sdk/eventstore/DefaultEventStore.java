package io.mubel.sdk.eventstore;

import io.mubel.api.grpc.v1.events.*;
import io.mubel.client.MubelClient;
import io.mubel.sdk.Constrains;
import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;

import java.util.List;

public class DefaultEventStore implements EventStore {

    private final String eventStoreId;
    private final MubelClient client;

    public static Builder builder() {
        return new Builder();
    }

    private DefaultEventStore(Builder b) {
        this.eventStoreId = b.eventStoreId;
        this.client = b.client;
    }

    @Override
    public void append(ExecuteRequest request) {
        client.execute(request);
    }

    @Override
    public List<EventData> get(String streamId) {
        var response = client.getEvents(
                GetEventsRequest.newBuilder()
                        .setEsid(eventStoreId)
                        .setSelector(EventSelector.newBuilder()
                                .setStream(StreamSelector.newBuilder()
                                        .setStreamId(streamId)
                                        .build())
                                .build())
                        .build()
        );
        return response.getEventList();
    }

    @Override
    public List<EventData> get(String streamId, int revision) {
        var response = client.getEvents(
                GetEventsRequest.newBuilder()
                        .setEsid(eventStoreId)
                        .setSelector(EventSelector.newBuilder()
                                .setStream(StreamSelector.newBuilder()
                                        .setStreamId(streamId)
                                        .setFromRevision(revision)
                                        .build())
                                .build())
                        .build()
        );
        return response.getEventList();
    }

    public static class Builder {
        private String eventStoreId;
        private MubelClient client;

        public Builder eventStoreId(String eventStoreId) {
            this.eventStoreId = eventStoreId;
            return this;
        }

        public Builder client(MubelClient client) {
            this.client = client;
            return this;
        }

        public DefaultEventStore build() {
            eventStoreId = validateEventStoreId();
            client = Constrains.requireNonNull(client, "Client");
            return new DefaultEventStore(this);
        }

        private String validateEventStoreId() {
            final var nn = Utils.requireNonNull(
                    eventStoreId,
                    () -> new MubelConfigurationException("Event store id may not be null")
            );
            return Utils.validate(
                    nn,
                    Constrains.ESID_PTRN,
                    () -> new MubelConfigurationException("Event store id must match <namespace>:<event store name> pattern %s".formatted(Constrains.ESID_PTRN))
            );
        }
    }
}
