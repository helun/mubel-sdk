package io.mubel.client;

import com.google.common.base.Throwables;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.mubel.api.grpc.*;
import io.mubel.client.internal.StreamObserverSubscription;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class MubelClient {

    private final EventServiceGrpc.EventServiceBlockingStub blockingStub;
    private final EventServiceGrpc.EventServiceStub asyncStub;

    public MubelClient(MubelClientConfig config) {
        var channel = ManagedChannelBuilder
                .forTarget(config.address())
                .executor(config.executor())
                .enableFullStreamDecompression()
                .enableRetry()
                .keepAliveTime(5, TimeUnit.SECONDS)
                .keepAliveTimeout(1, TimeUnit.SECONDS)
                .usePlaintext()
                .build();
        blockingStub = EventServiceGrpc.newBlockingStub(channel);
        asyncStub = EventServiceGrpc.newStub(channel);
    }

    /**
     * Provisions a new event store. The call will fail if the event store already exists.
     */
    public EventStoreDetails provision(ProvisionEventStoreRequest request) {
        try {
            return blockingStub.provision(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    /**
     * Append new events to an event store
     */
    public AppendAck append(AppendRequest request) {
        try {
            return blockingStub.append(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public GetEventsResponse get(GetEventsRequest request) {
        try {
            return blockingStub.get(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public Subscription subscribe(SubscribeRequest request, int bufferSize) {
        final var subscription = new StreamObserverSubscription(bufferSize);
        asyncStub.subscribe(request, subscription);
        return subscription;
    }

    public ServiceInfoResponse getServerInfo() {
        try {
            return blockingStub.serverInfo(GetServiceInfoRequest.newBuilder().build());
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public DropEventStoreResponse drop(DropEventStoreRequest request) {
        try {
            return blockingStub.drop(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public void scheduleEvent(ScheduledEvent event) {
        try {
            final var empty = blockingStub.scheduleEvent(event);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    /**
     * Subscribe to scheduled events. The subscriber will receive events when they are published.
     */
    public Iterator<TriggeredEvents> subscribeToScheduledEvents(ScheduledEventsSubscribeRequest request) {
        try {
            return blockingStub.subscribeToScheduledEvents(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public void cancelScheduledEvents(CancelScheduledEventsRequest request) {
        try {
            final var empty = blockingStub.cancelScheduledEvents(request);
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
