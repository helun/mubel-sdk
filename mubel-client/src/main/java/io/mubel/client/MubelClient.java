package io.mubel.client;

import com.google.common.base.Throwables;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.mubel.api.grpc.v1.common.ProblemDetail;
import io.mubel.api.grpc.v1.events.*;
import io.mubel.api.grpc.v1.groups.*;
import io.mubel.api.grpc.v1.server.*;
import io.mubel.client.exceptions.MubelClientException;
import io.mubel.client.internal.StreamObserverFluxAdapter;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MubelClient {

    private final MubelEventsServiceGrpc.MubelEventsServiceBlockingStub blockingEventsServiceStub;
    private final MubelEventsServiceGrpc.MubelEventsServiceStub asyncEventsServiceStub;
    private final MubelServerGrpc.MubelServerBlockingStub blockingServerStub;
    private final GroupsServiceGrpc.GroupsServiceStub asyncGroupsServiceStub;
    private final GroupsServiceGrpc.GroupsServiceBlockingStub blockingGroupsServiceStub;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("mubel-client-background-task" + thread.threadId());
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
        blockingEventsServiceStub = MubelEventsServiceGrpc.newBlockingStub(channel);
        asyncEventsServiceStub = MubelEventsServiceGrpc.newStub(channel);
        blockingServerStub = MubelServerGrpc.newBlockingStub(channel);
        asyncGroupsServiceStub = GroupsServiceGrpc.newStub(channel);
        blockingGroupsServiceStub = GroupsServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Provisions a new event store. The call will fail if the event store already exists.
     */
    public CompletableFuture<EventStoreDetails> provision(ProvisionEventStoreRequest request) {
        try {
            var jobStatus = blockingServerStub.provision(request);
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
        var serverInfo = blockingServerStub.serverInfo(GetServiceInfoRequest.newBuilder().build());
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
                    jobStatus = blockingServerStub.jobStatus(GetJobStatusRequest.newBuilder()
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
     * Execute operations on the event store.
     */
    public void execute(ExecuteRequest request) {
        try {
            var ignored = blockingEventsServiceStub.execute(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public GetEventsResponse getEvents(GetEventsRequest request) {
        try {
            return blockingEventsServiceStub.getEvents(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public Flux<EventData> getEventStream(GetEventsRequest request) {
        final StreamObserverFluxAdapter<EventData> adapter = new StreamObserverFluxAdapter<>();
        asyncEventsServiceStub.getEventStream(request, adapter);
        return adapter.toFlux();
    }

    public Flux<EventData> subscribe(SubscribeRequest request) {
        final StreamObserverFluxAdapter<EventData> adapter = new StreamObserverFluxAdapter<>();
        asyncEventsServiceStub.subscribe(request, adapter);
        return adapter.toFlux();
    }

    public ServiceInfoResponse getServerInfo() {
        try {
            return blockingServerStub.serverInfo(GetServiceInfoRequest.newBuilder().build());
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public EventStoreSummary eventStoreSummary(GetEventStoreSummaryRequest request) {
        try {
            return blockingServerStub.eventStoreSummary(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public CompletableFuture<Void> drop(DropEventStoreRequest request) {
        try {
            var jobStatus = blockingServerStub.drop(request);
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

    /**
     * Subscribe to scheduled events. The subscriber will receive events when they are published.
     */
    public Flux<Deadline> subscribeToDeadlines(DeadlineSubscribeRequest request) {
        var adapter = new StreamObserverFluxAdapter<Deadline>();
        asyncEventsServiceStub.subcribeToDeadlines(request, adapter);
        return adapter.toFlux();
    }

    public Flux<GroupStatus> joinConsumerGroup(JoinGroupRequest request) {
        var adapter = new StreamObserverFluxAdapter<GroupStatus>();
        asyncGroupsServiceStub.join(request, adapter);
        return adapter.toFlux();
    }

    public void leaveConsumerGroup(LeaveGroupRequest request) {
        try {
            var ignored = blockingGroupsServiceStub.leaveConsumerGroup(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public void heartbeat(Heartbeat heartbeat) {
        try {
            var ignored = blockingGroupsServiceStub.heartbeat(heartbeat);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public JobStatus copyEvents(CopyEventsRequest request) {
        try {
            return blockingServerStub.copyEvents(request);
        } catch (Throwable err) {
            throw handleFailure(err);
        }
    }

    public JobStatus jobStatus(GetJobStatusRequest request) {
        try {
            return blockingServerStub.jobStatus(request);
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
