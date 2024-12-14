package io.mubel.sdk.execution;

import io.mubel.api.grpc.v1.events.CancelScheduledOperation;
import io.mubel.api.grpc.v1.events.EntityReference;
import io.mubel.api.grpc.v1.events.ExecuteRequest;
import io.mubel.api.grpc.v1.events.Operation;
import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.HandlerResult;
import io.mubel.sdk.eventstore.EventStore;
import io.mubel.sdk.exceptions.EventStreamNotFoundException;
import io.mubel.sdk.execution.internal.CommandExecutor;
import io.mubel.sdk.execution.internal.InvocationContext;
import io.mubel.sdk.scheduled.ExpiredDeadline;
import io.mubel.sdk.scheduled.ExpiredDeadlineConsumer;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class AggregateInvocationService<T, E, C> implements ExpiredDeadlineConsumer {
    private final EventStore eventStore;
    private final CommandExecutor<T, E, C> commandExecutor;
    private final EventDataMapper eventDataMapper;
    private final String aggregateName;

    public AggregateInvocationService(
            AggregateInvocationConfig<T, E, C> config,
            EventStore eventStore,
            EventDataMapper eventDataMapper
    ) {
        this.eventStore = requireNonNull(eventStore, "eventStore may not be null");
        this.eventDataMapper = requireNonNull(eventDataMapper, "eventDataMapper may not be null");
        this.commandExecutor = new CommandExecutor<>(requireNonNull(config, "config may not be null"));
        this.aggregateName = config.aggregateName();
    }

    public CommandResult<E> submit(UUID streamId, C command) {
        final var nnStreamId = parseStreamId(streamId);
        final var nnCommand = requireNonNull(command, "command may not be null");
        final var ctx = InvocationContext.create(nnStreamId);
        var existingEvents = getExistingEvents(nnStreamId, ctx);
        final var handlerResult = commandExecutor.execute(existingEvents, nnCommand);
        final int oldVersion = ctx.currentVersion();
        applyResult(ctx, handlerResult);
        final int newVersion = ctx.currentVersion();
        return new CommandResult<>(
                streamId,
                handlerResult.events().size(),
                oldVersion,
                newVersion,
                handlerResult.events()
        );
    }

    @Override
    public void deadlineExpired(ExpiredDeadline expiredDeadline) {
        final var nnStreamId = parseStreamId(expiredDeadline.targetEntityId());
        final var ctx = InvocationContext.create(nnStreamId);
        final var existingEvents = getExistingEvents(nnStreamId, ctx);
        final var handlerResult = commandExecutor.handleExpiredDeadline(existingEvents, expiredDeadline);
        applyResult(ctx, handlerResult);
    }

    public T getState(UUID streamId) {
        return findState(streamId).orElseThrow(() -> new EventStreamNotFoundException(streamId));
    }

    public T getState(UUID streamId, int version) {
        final var nnStreamId = parseStreamId(streamId);
        final var ctx = InvocationContext.create(nnStreamId);
        return commandExecutor.getState(getExistingEvents(nnStreamId, version, ctx))
                .orElseThrow(() -> new EventStreamNotFoundException(streamId));
    }

    public Optional<T> findState(UUID streamId) {
        final var nnStreamId = parseStreamId(streamId);
        final var ctx = InvocationContext.create(nnStreamId);
        return commandExecutor.getState(getExistingEvents(nnStreamId, ctx));
    }

    @Override
    public String targetType() {
        return aggregateName;
    }

    private static String parseStreamId(UUID streamId) {
        return requireNonNull(streamId, "streamId may not be null").toString();
    }

    private void applyResult(InvocationContext ctx, HandlerResult<E> result) {
        if (result.isEmpty()) {
            return;
        }
        var exrb = ExecuteRequest.newBuilder();
        result.events(events -> {
            final var appendOp = eventDataMapper.toAppendOp(
                    ctx.streamId(),
                    events,
                    ctx::nextVersion);
            exrb.addOperation(appendOp);
        });

        final var aggregateReference = EntityReference.newBuilder()
                .setId(ctx.streamId())
                .setType(aggregateName)
                .build();

        result.deadlines(deadlines ->
                exrb.addAllOperation(eventDataMapper.toDeadlineOps(
                        aggregateReference,
                        deadlines
                ))
        );

        result.scheduledEvents(scheduled ->
                exrb.addAllOperation(eventDataMapper.toScheduledEventOps(
                        aggregateReference,
                        scheduled
                )));

        result.cancelIds(cancelIds -> exrb.addOperation(Operation.newBuilder().setCancel(CancelScheduledOperation.newBuilder()
                        .addAllEventId(cancelIds)
                        .build())
                .build())
        );
        eventStore.execute(exrb);
    }

    private Flux<E> getExistingEvents(String streamId, InvocationContext ctx) {
        return (Flux<E>) eventStore.getAsync(streamId)
                .doOnNext(ed -> {
                    ctx.applyRevision(ed.getRevision());
                })
                .map(eventDataMapper::fromEventData);
    }

    @SuppressWarnings("unchecked")
    private Flux<E> getExistingEvents(String streamId, int version, InvocationContext ctx) {
        return (Flux<E>) eventStore.getAsync(streamId, version)
                .doOnNext(ed -> ctx.applyRevision(ed.getRevision()))
                .map(eventDataMapper::fromEventData);
    }

}
