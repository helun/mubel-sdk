package io.mubel.sdk.execution;

import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.HandlerResult;
import io.mubel.sdk.eventstore.AppendRequest;
import io.mubel.sdk.eventstore.EventStore;
import io.mubel.sdk.exceptions.EventStreamNotFoundException;
import io.mubel.sdk.execution.internal.CommandExecutor;
import io.mubel.sdk.execution.internal.InvocationContext;
import io.mubel.sdk.scheduled.ExpiredDeadline;
import io.mubel.sdk.scheduled.ExpiredDeadlineConsumer;

import java.util.List;
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
        final var existingEvents = getExistingEvents(nnStreamId, ctx);
        final int oldVersion = ctx.currentVersion();
        final var handlerResult = commandExecutor.execute(existingEvents, nnCommand);
        appendResult(ctx, handlerResult);
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
    public void accept(ExpiredDeadline expiredDeadline) {
        final var nnStreamId = parseStreamId(expiredDeadline.targetEntityId());
        final var ctx = InvocationContext.create(nnStreamId);
        final var existingEvents = getExistingEvents(nnStreamId, ctx);
        final var handlerResult = commandExecutor.handleExpiredDeadline(existingEvents, expiredDeadline);
        appendResult(ctx, handlerResult);
    }

    public T getState(UUID streamId) {
        final var nnStreamId = parseStreamId(streamId);
        final var ctx = InvocationContext.create(nnStreamId);
        final var existingEvents = getExistingEvents(nnStreamId, ctx);
        if (existingEvents.isEmpty()) {
            throw new EventStreamNotFoundException("stream id: %s not found".formatted(streamId));
        }
        return commandExecutor.getState(existingEvents);
    }

    public T getState(UUID streamId, int version) {
        final var nnStreamId = parseStreamId(streamId);
        final var ctx = InvocationContext.create(nnStreamId);
        final var existingEvents = getExistingEvents(nnStreamId, version, ctx);
        if (existingEvents.isEmpty()) {
            throw new EventStreamNotFoundException("stream id: %s not found".formatted(streamId));
        }
        return commandExecutor.getState(existingEvents);
    }

    @Override
    public String targetType() {
        return aggregateName;
    }

    private static String parseStreamId(UUID streamId) {
        return requireNonNull(streamId, "streamId may not be null").toString();
    }

    public Optional<T> findState(UUID streamId) {
        final var nnStreamId = parseStreamId(streamId);
        final var ctx = InvocationContext.create(nnStreamId);
        final var existingEvents = getExistingEvents(nnStreamId, ctx);
        if (existingEvents.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(commandExecutor.getState(existingEvents));
    }

    private void appendResult(InvocationContext ctx, HandlerResult<E> result) {
        if (result.isEmpty()) {
            return;
        }

        final var events = eventDataMapper.toEventDataInput(
                ctx.streamId(),
                result.events(),
                ctx::nextVersion);
        final var scheduledEvents = eventDataMapper.toScheduledEvent(
                ctx.streamId(),
                aggregateName,
                result
        );

        eventStore.append(
                new AppendRequest(
                        events,
                        scheduledEvents,
                        result.cancelIds()
                )
        );
    }

    @SuppressWarnings("unchecked")
    private List<E> getExistingEvents(String streamId, InvocationContext ctx) {
        return (List<E>) eventDataMapper.fromEventData(
                eventStore.get(streamId),
                ed -> ctx.applyVersion(ed.getVersion())
        );
    }

    @SuppressWarnings("unchecked")
    private List<E> getExistingEvents(String streamId, int version, InvocationContext ctx) {
        return (List<E>) eventDataMapper.fromEventData(
                eventStore.get(streamId, version),
                ed -> ctx.applyVersion(ed.getVersion())
        );
    }

}
