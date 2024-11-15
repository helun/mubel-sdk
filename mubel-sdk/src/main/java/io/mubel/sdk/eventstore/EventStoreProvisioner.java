package io.mubel.sdk.eventstore;

import io.mubel.api.grpc.v1.server.EventStoreDetails;
import io.mubel.api.grpc.v1.server.ProvisionEventStoreRequest;
import io.mubel.api.grpc.v1.server.ServiceInfoResponse;
import io.mubel.api.grpc.v1.server.StorageBackendInfo;
import io.mubel.client.MubelClient;
import io.mubel.sdk.Constrains;
import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EventStoreProvisioner {

    private final MubelClient client;
    private final List<ProvisionParams> eventStores;

    private EventStoreProvisioner(Builder builder) {
        this.client = builder.client;
        this.eventStores = builder.eventStores;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void provision() {
        final var serverInfo = client.getServerInfo();
        final var byEsid = indexEventStoreByEsid(serverInfo);
        final var backends = getBackends(serverInfo);
        eventStores.stream()
                .filter(pp -> verifyParams(pp, byEsid, backends))
                .map(mapRequest())
                .forEach(client::provision);
    }

    private static Set<String> getBackends(ServiceInfoResponse serverInfo) {
        return serverInfo.getStorageBackendList()
                .stream()
                .map(StorageBackendInfo::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Function<ProvisionParams, ProvisionEventStoreRequest> mapRequest() {
        return pp -> ProvisionEventStoreRequest.newBuilder()
                .setEsid(pp.eventStoreId())
                .setDataFormat(mapDataFormat(pp.dataFormat()))
                .build();
    }

    private static boolean verifyParams(ProvisionParams pp, Map<String, EventStoreDetails> byEsid, Set<String> backends) {
        if (!byEsid.containsKey(pp.eventStoreId())) {
            final var parts = pp.eventStoreId().split(":");
            final var namespace = parts[0];
            if (backends.isEmpty()) {
                throw new MubelConfigurationException("No backends available");
            }
            if (!backends.contains(namespace)) {
                final var backendString = String.join(", ", backends);
                final var exampleEsid = backends.stream().findFirst()
                        .map(ns -> ns + ":" + parts[1])
                        .orElseThrow();

                final var errorMessage = "Event store id: %s references non-existing backend. Available backends: [%s]. Example: %s"
                        .formatted(pp.eventStoreId(), backendString, exampleEsid);
                throw new MubelConfigurationException(errorMessage);
            }
            return true;
        }
        final var details = byEsid.get(pp.eventStoreId());
        if (details.getDataFormat() != mapDataFormat(pp.dataFormat())) {
            throw new MubelConfigurationException("Event store " + pp.eventStoreId() + " already exists with different data format");
        }
        return false;
    }

    private static Map<String, EventStoreDetails> indexEventStoreByEsid(ServiceInfoResponse serverInfo) {
        return serverInfo.getEventStoreList()
                .stream()
                .collect(Collectors.toMap(EventStoreDetails::getEsid, Function.identity()));
    }

    private static io.mubel.api.grpc.v1.server.DataFormat mapDataFormat(DataFormat dataFormat) {
        return switch (dataFormat) {
            case JSON -> io.mubel.api.grpc.v1.server.DataFormat.JSON;
            case PROTOBUF -> io.mubel.api.grpc.v1.server.DataFormat.PROTO;
            case OTHER -> io.mubel.api.grpc.v1.server.DataFormat.OTHER;
        };
    }


    public static class Builder {

        private MubelClient client;
        private final List<ProvisionParams> eventStores = new ArrayList<>();

        public Builder client(MubelClient client) {
            this.client = client;
            return this;
        }

        public Builder eventStore(String eventStoreId, DataFormat dataFormat) {
            return eventStore(new ProvisionParams(eventStoreId, dataFormat, true));
        }

        public Builder eventStore(ProvisionParams params) {
            eventStores.add(params);
            return this;
        }

        public EventStoreProvisioner build() {
            Utils.assertNotEmpty(eventStores, () -> new MubelConfigurationException("Event stores may not be empty"));
            client = Constrains.requireNonNull(client, "Client");
            return new EventStoreProvisioner(this);
        }

    }

    /**
     * Specifies the format of the serialized events in the event store.
     */
    public enum DataFormat {
        JSON,
        PROTOBUF,
        OTHER
    }

    public record ProvisionParams(
            String eventStoreId,
            DataFormat dataFormat,
            boolean waitForOpen
    ) {
        public ProvisionParams {
            eventStoreId = Constrains.validateEventStoreId(eventStoreId);
            dataFormat = Constrains.requireNonNull(dataFormat, "dataFormat");
        }
    }

}
