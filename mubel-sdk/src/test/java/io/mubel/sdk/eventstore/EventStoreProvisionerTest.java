package io.mubel.sdk.eventstore;

import io.mubel.api.grpc.DataFormat;
import io.mubel.api.grpc.EventStoreDetails;
import io.mubel.api.grpc.ServiceInfoResponse;
import io.mubel.api.grpc.StorageBackendInfo;
import io.mubel.client.MubelClient;
import io.mubel.sdk.exceptions.MubelConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventStoreProvisionerTest {

    static String ESID = "backend:event_store_name";

    static EventStoreDetails PROTO_DETAILS = EventStoreDetails.newBuilder()
            .setEsid(ESID)
            .setType("PG")
            .setDataFormat(DataFormat.PROTO)
            .build();

    static EventStoreDetails JSON_DETAILS = EventStoreDetails.newBuilder()
            .setEsid(ESID)
            .setType("PG")
            .setDataFormat(DataFormat.JSON)
            .build();

    MubelClient client = mock(MubelClient.class);

    @BeforeEach
    void setup() {
        when(client.provision(any())).thenReturn(CompletableFuture.completedFuture(PROTO_DETAILS));
    }

    @Test
    void provisionExistingWithSameParams() {
        EventStoreProvisioner eventStoreProvisioner = EventStoreProvisioner.builder()
                .eventStore(ESID, EventStoreProvisioner.DataFormat.PROTOBUF)
                .client(client)
                .build();

        setupServiceInfo(ServiceInfoResponse.newBuilder()
                .addEventStore(PROTO_DETAILS));

        eventStoreProvisioner.provision();
        verifyNoProvisionCall();
    }

    @Test
    void provisionExistingWithDifferentParams() {
        EventStoreProvisioner eventStoreProvisioner = EventStoreProvisioner.builder()
                .eventStore(ESID, EventStoreProvisioner.DataFormat.PROTOBUF)
                .client(client)
                .build();

        setupServiceInfo(ServiceInfoResponse.newBuilder()
                .addEventStore(JSON_DETAILS));

        assertThatThrownBy(eventStoreProvisioner::provision)
                .as("provisioning an existing event store with different params should fail")
                .isInstanceOf(MubelConfigurationException.class);
        verifyNoProvisionCall();
    }

    @Test
    void provisionNew() {
        EventStoreProvisioner eventStoreProvisioner = EventStoreProvisioner.builder()
                .eventStore(ESID, EventStoreProvisioner.DataFormat.PROTOBUF)
                .client(client)
                .build();

        setupServiceInfo(ServiceInfoResponse.newBuilder()
                .addStorageBackend(StorageBackendInfo.newBuilder()
                        .setType("PG")
                        .setName("backend")
                        .build())
        );
        eventStoreProvisioner.provision();
        verifyProvisionCall();
    }

    @Test
    void provisionAgainstNonExistingBackendShouldFail() {
        EventStoreProvisioner eventStoreProvisioner = EventStoreProvisioner.builder()
                .eventStore(ESID, EventStoreProvisioner.DataFormat.PROTOBUF)
                .client(client)
                .build();

        setupServiceInfo(
                ServiceInfoResponse.newBuilder()
                        .addStorageBackend(StorageBackendInfo.newBuilder()
                                .setType("PG")
                                .setName("other_backend")
                                .build())
        );

        assertThatThrownBy(eventStoreProvisioner::provision)
                .as("provisioning against non existing backend should fail")
                .isInstanceOf(MubelConfigurationException.class);

        verifyNoProvisionCall();
    }

    private void verifyProvisionCall() {
        verify(client, times(1)).provision(any());
    }

    private void setupServiceInfo(ServiceInfoResponse.Builder JSON_DETAILS) {
        when(client.getServerInfo()).thenReturn(JSON_DETAILS
                .build()
        );
    }

    private void verifyNoProvisionCall() {
        verify(client, never()).provision(any());
    }
}