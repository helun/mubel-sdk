package io.mubel.client;

import com.google.common.base.Throwables;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.mubel.api.grpc.*;

import java.util.Iterator;

public class MubelClient {

    private final EventServiceGrpc.EventServiceBlockingStub serviceStub;

    public MubelClient(MubelClientConfig config) {
        var channel = ManagedChannelBuilder
                .forAddress(config.host(), config.port())
                .usePlaintext()
                .build();
        serviceStub = EventServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Provisions a new event store. The call will fail if the event store already exists.
     */
    public EventStoreDetails provision(ProvisionEventStoreRequest request) {
        try {
            return serviceStub.provision(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    /**
     * Append new events to an event store
     */
    public AppendAck append(AppendRequest request) {
        try {
            return serviceStub.append(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public GetEventsResponse get(GetEventsRequest request) {
        try {
            return serviceStub.get(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public Iterator<EventData> subscribe(SubscribeRequest request) {
        try {
            return serviceStub.subscribe(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public ServiceInfoResponse getServerInfo() {
        try {
            return serviceStub.serverInfo(GetServiceInfoRequest.newBuilder().build());
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public DropEventStoreResponse drop(DropEventStoreRequest request) {
        try {
            return serviceStub.drop(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public void scheduleEvent(ScheduledEvent event) {
        try {
            final var empty = serviceStub.scheduleEvent(event);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    /**
     * Subscribe to scheduled events. The subscriber will receive events when they are published.
     */
    public Iterator<TriggeredEvents> subscribeToScheduledEvents(ScheduledEventsSubscribeRequest request) {
        try {
            return serviceStub.subscribeToScheduledEvents(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public void cancelScheduledEvents(CancelScheduledEventsRequest request) {
        try {
            final var empty = serviceStub.cancelScheduledEvents(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    private RuntimeException handleFailure(Throwable err) {
        try {
            if (err == null) {
                throw new RuntimeException("unknown failure");
            }
            final var cause = Throwables.getRootCause(err);
            if (cause instanceof io.grpc.StatusRuntimeException sre) {
                final var metadata = Status.trailersFromThrowable(sre);
                if (metadata != null) {
                    var pd = metadata.get(ProtoUtils.keyForProto(ProblemDetail.getDefaultInstance()));
                    System.err.println(pd);
                }
                return sre;
            }
        } catch (Throwable t) {
            return new RuntimeException(t);
        }

        return new RuntimeException(err);
    }
}
