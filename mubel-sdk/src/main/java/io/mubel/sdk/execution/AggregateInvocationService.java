package io.mubel.sdk.execution;

import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.eventstore.EventStore;
import io.mubel.sdk.exceptions.EventStreamNotFoundException;
import io.mubel.sdk.execution.internal.CommandExecutor;
import io.mubel.sdk.execution.internal.InvocationContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class AggregateInvocationService<T, E, C> {

    private final EventStore eventStore;
    private final CommandExecutor<T, E, C> commandExecutor;
    private final EventDataMapper eventDataMapper;

    public AggregateInvocationService(
            AggregateInvocationConfig<T, E, C> config,
            EventStore eventStore,
            EventDataMapper eventDataMapper
    ) {
        this.eventStore = requireNonNull(eventStore, "eventStore may not be null");
        this.eventDataMapper = requireNonNull(eventDataMapper, "eventDataMapper may not be null");
        this.commandExecutor = new CommandExecutor<>(requireNonNull(config, "config may not be null"));
    }

    public CommandResult submit(UUID streamId, C command) {
        final var nnStreamId = parseStreamId(streamId);
        final var nnCommand = requireNonNull(command, "command may not be null");
        final var ctx = InvocationContext.create(nnStreamId);
        final var existingEvents = getExistingEvents(nnStreamId, ctx);
        final int oldVersion = ctx.currentVersion();
        final var newEvents = commandExecutor.execute(existingEvents, nnCommand);
        appendNewEvents(ctx, newEvents);
        final int newVersion = ctx.currentVersion();
        return new CommandResult(
                streamId,
                newEvents.size(),
                oldVersion,
                newVersion
        );
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

    private void appendNewEvents(InvocationContext ctx, List<E> newEvents) {
        if (newEvents.isEmpty()) {
            return;
        }
        eventStore.append(eventDataMapper.toEventDataInput(
                ctx.streamId(),
                newEvents,
                ctx::nextVersion)
        );
    }

    @SuppressWarnings("unchecked")
    private List<E> getExistingEvents(String streamId, InvocationContext ctx) {
        return (List<E>) eventDataMapper.fromEventData(
                eventStore.get(streamId),
                ed -> ctx.applyVersion(ed.getVersion())
        );
    }

}
