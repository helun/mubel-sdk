package io.mubel.client;

import com.google.common.base.Throwables;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.mubel.api.grpc.*;
import io.mubel.client.internal.StreamObserverFuture;
import io.mubel.client.internal.StreamObserverSubscription;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
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

    public Subscription<EventData> subscribe(SubscribeRequest request, int bufferSize) {
        final var subscription = new StreamObserverSubscription<EventData>(bufferSize);
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
    public Subscription<TriggeredEvents> subscribeToScheduledEvents(ScheduledEventsSubscribeRequest request, int bufferSize) {
        final var subscription = new StreamObserverSubscription<TriggeredEvents>(bufferSize);
        asyncStub.subscribeToScheduledEvents(request, subscription);
        return subscription;
    }

    public void cancelScheduledEvents(CancelScheduledEventsRequest request) {
        try {
            final var empty = blockingStub.cancelScheduledEvents(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public Future<ConsumerGroupStatus> joinConsumerGroup(JoinConsumerGroupRequest request) {
        CompletableFuture<ConsumerGroupStatus> future = new CompletableFuture<>();
        final var call = asyncStub.getChannel().newCall(
                EventServiceGrpc.getJoinConsumerGroupMethod(),
                asyncStub.getCallOptions()
        );
        io.grpc.stub.ClientCalls.asyncServerStreamingCall(
                call, request, new StreamObserverFuture<>(future));

        future.whenComplete((status, err) -> {
            if (err instanceof CancellationException cerr) {
                call.cancel("cancelled", null);
            }
        });
        return future;
    }

    public void leaveConsumerGroup(LeaveConsumerGroupRequest request) {
        try {
            final var empty = blockingStub.leaveConsumerGroup(request);
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
