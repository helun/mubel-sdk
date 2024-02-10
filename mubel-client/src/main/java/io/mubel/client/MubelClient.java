package io.mubel.client;

import com.google.common.base.Throwables;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.mubel.api.grpc.*;
import io.mubel.client.exceptions.MubelClientException;
import io.mubel.client.internal.MubelSystemEventFilter;
import io.mubel.client.internal.StreamObserverFuture;
import io.mubel.client.internal.StreamObserverSubscription;

import java.util.concurrent.*;

public class MubelClient {

    private final EventServiceGrpc.EventServiceBlockingStub blockingStub;
    private final EventServiceGrpc.EventServiceStub asyncStub;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("mubel-client-background-task" + thread.getId());
        return thread;
    });

    public MubelClient(MubelClientConfig config) {
        var channel = ManagedChannelBuilder
                .forTarget(config.address())
                .executor(config.executor())
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
    public CompletableFuture<EventStoreDetails> provision(ProvisionEventStoreRequest request) {
        try {
            var jobStatus = blockingStub.provision(request);
            if (jobStatus.getState() == JobState.FAILED) {
                return CompletableFuture.failedFuture(ExceptionHandler.mapProblem(jobStatus.getProblem()));
            } else if (jobStatus.getState() == JobState.COMPLETED) {
                var details = getEventStoreDetails(request.getEsid());
                return CompletableFuture.completedFuture(details);
            } else {
                return awaitJobCompletion(jobStatus.getJobId())
                        .thenApply(ignored -> getEventStoreDetails(request.getEsid()));

            }
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    private EventStoreDetails getEventStoreDetails(String esid) {
        var serverInfo = blockingStub.serverInfo(GetServiceInfoRequest.newBuilder().build());
        return serverInfo.getEventStoreList()
                .stream()
                .filter(es -> es.getEsid().equals(esid))
                .findFirst()
                .orElseThrow(() -> new MubelClientException("event store %s not found".formatted(esid)));
    }

    private CompletableFuture<Void> awaitJobCompletion(String jobId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JobStatus jobStatus = null;
                var state = JobState.RUNNING;
                while (state == JobState.RUNNING) {
                    Thread.sleep(1000);
                    jobStatus = blockingStub.jobStatus(GetJobStatusRequest.newBuilder()
                            .setJobId(jobId)
                            .build());
                    state = jobStatus.getState();
                }
                if (state == JobState.FAILED) {
                    throw ExceptionHandler.mapProblem(jobStatus.getProblem());
                }
            } catch (Throwable err) {
                throw handleFailure(err);
            }
            return null;
        }, executor);
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
        final var subscription = new StreamObserverSubscription<>(
                bufferSize,
                MubelSystemEventFilter.eventDataEventErrorChecker()
        );
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

    public EventStoreSummary eventStoreSummary(GetEventStoreSummaryRequest request) {
        try {
            return blockingStub.eventStoreSummary(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public CompletableFuture<Void> drop(DropEventStoreRequest request) {
        try {
            var jobStatus = blockingStub.drop(request);
            if (jobStatus.getState() == JobState.FAILED) {
                return CompletableFuture.failedFuture(ExceptionHandler.mapProblem(jobStatus.getProblem()));
            } else if (jobStatus.getState() == JobState.COMPLETED) {
                return CompletableFuture.completedFuture(null);
            } else {
                return awaitJobCompletion(jobStatus.getJobId());
            }
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
        final var subscription = new StreamObserverSubscription<>(
                bufferSize,
                MubelSystemEventFilter.scheduledEventErrorChecker()
        );
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

    public JobStatus copyEvents(CopyEventsRequest request) {
        try {
            return blockingStub.copyEvents(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public JobStatus jobStatus(GetJobStatusRequest request) {
        try {
            return blockingStub.jobStatus(request);
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
