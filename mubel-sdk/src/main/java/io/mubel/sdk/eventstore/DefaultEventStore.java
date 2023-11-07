package io.mubel.sdk.eventstore;


import io.mubel.api.grpc.AppendRequest;
import io.mubel.api.grpc.EventData;
import io.mubel.api.grpc.EventDataInput;
import io.mubel.api.grpc.GetEventsRequest;
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
    public void append(List<EventDataInput> events) {
        client.append(
                AppendRequest.newBuilder()
                        .setEsid(eventStoreId)
                        .addAllEvents(events)
                        .build()
        );
    }

    @Override
    public List<EventData> get(String streamId) {
        var response = client.get(
                GetEventsRequest.newBuilder()
                        .setEsid(eventStoreId)
                        .setStreamId(streamId)
                        .build()
        );
        return response.getEventsList();
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
