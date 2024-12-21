package io.mubel.sdk;

import com.google.protobuf.ByteString;
import io.mubel.api.grpc.v1.events.*;
import io.mubel.sdk.codec.EventDataCodec;
import io.mubel.sdk.internal.UuidUtil;
import io.mubel.sdk.scheduled.ExpiredDeadline;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A mapper for event data.
 *
 * This class is used to map event data to event classes and vice versa.
 */
public class EventDataMapper {

    private final EventDataCodec codec;
    private final EventTypeRegistry eventTypeRegistry;
    private final IdGenerator idGenerator;
    private final Clock clock = Clock.systemUTC();

    public EventDataMapper(
            EventDataCodec codec,
            EventTypeRegistry eventTypeRegistry,
            IdGenerator idGenerator) {
        this.codec = requireNonNull(codec, "codec may not be null");
        this.eventTypeRegistry = requireNonNull(eventTypeRegistry, "eventTypeRegistry may not be null");
        this.idGenerator = requireNonNull(idGenerator, "idGenerator may not be null");
    }

    /**
     * Deserializes the event data and applies the consumer to the event data.
     *
     * @param eventData         event data to deserialize
     * @param eventDataConsumer is called after the event data is deserialized
     * @return deserialized event data
     */
    public Object fromEventData(EventData eventData, Consumer<EventData> eventDataConsumer) {
        final var decoded = fromEventData(eventData);
        eventDataConsumer.accept(eventData);
        return decoded;
    }

    /**
     * Deserializes the event data.
     *
     * @param eventData The event data to deserialize.
     * @return The deserialized event.
     */
    public Object fromEventData(EventData eventData) {
        final var eventClass = eventTypeRegistry.getClassForType(eventData.getType());
        return codec.decode(eventData.getData().toByteArray(), eventClass);
    }

    /**
     * Maps a deadline to an ExpiredDeadline.
     *
     * @param deadline The deadline to map.
     * @param timestamp The timestamp of the event.
     * @return The expired deadline.
     */
    @SuppressWarnings("unchecked")
    public ExpiredDeadline mapExpiredDeadline(io.mubel.api.grpc.v1.events.Deadline deadline, Instant timestamp) {
        final Map<String, String> attributes;
        if (deadline.getData().isEmpty()) {
            attributes = null;
        } else {
            attributes = codec.decode(deadline.getData().toByteArray(), Map.class);
        }
        return new ExpiredDeadline(
                UuidUtil.parseUuid(deadline.getTargetEntity().getId()),
                deadline.getType(),
                attributes,
                timestamp
        );
    }

    /**
     * Deserializes the event data and applies the consumer to the event data.
     *
     * @param events            event data to deserialize
     * @param eventDataConsumer is called after the event data is deserialized
     * @return deserialized event data
     */
    public List<?> fromEventData(List<EventData> events, Consumer<EventData> eventDataConsumer) {
        return events.stream().map(e -> fromEventData(e, eventDataConsumer)).toList();
    }

    /**
     * Creates an append operation for a list of events.
     *
     * @param streamId The stream id.
     * @param events The events to append.
     * @param versionSupplier The version supplier.
     * @return The append operation.
     */
    public Operation toAppendOp(String streamId, List<?> events, Supplier<Integer> versionSupplier) {
        final var builder = EventDataInput.newBuilder()
                .setStreamId(requireNonNull(streamId, "streamId may not be null"));
        var ed = events.stream()
                .map(e -> toAppendOp(builder, e, versionSupplier))
                .toList();
        return Operation.newBuilder()
                .setAppend(AppendOperation.newBuilder()
                        .addAllEvent(ed)
                        .build())
                .build();
    }

    /**
     * Creates scheduled event operations for a list of scheduled events.
     *
     * @param entityReference The entity reference.
     * @param input The scheduled events.
     * @return The scheduled event operations.
     */
    public <T> Iterable<Operation> toScheduledEventOps(EntityReference entityReference, List<HandlerResult.ScheduledEvent<T>> input) {
        if (input.isEmpty()) {
            return List.of();
        }
        var eventDataBuilder = EventDataInput.newBuilder()
                .setStreamId(entityReference.getId());
        return input
                .stream()
                .map(se -> Operation.newBuilder().setScheduleEvent(ScheduleEventOperation.newBuilder()
                                .setEvent(toAppendOp(eventDataBuilder, se.event(), () -> -1))
                                .setPublishTime(clock.instant().plus(se.duration()).toEpochMilli())
                                .build())
                        .build()
                ).toList();
    }

    /**
     * Creates deadline operations for a list of deadlines.
     *
     * @param entityReference The entity reference.
     * @param deadlines The deadlines to create operations for.
     * @return The deadline operations.
     */
    public List<Operation> toDeadlineOps(EntityReference entityReference, List<Deadline> deadlines) {
        if (deadlines.isEmpty()) {
            return List.of();
        }
        return deadlines.stream()
                .map(toScheduleDeadlineOp(entityReference))
                .toList();
    }

    private Function<Deadline, Operation> toScheduleDeadlineOp(EntityReference entityReference) {
        return dl -> Operation.newBuilder().setScheduleDeadline(ScheduleDeadlineOperation.newBuilder()
                        .setDeadline(io.mubel.api.grpc.v1.events.Deadline.newBuilder()
                                .setData(ByteString.copyFrom(codec.encode(dl.attributes())))
                                .setTargetEntity(entityReference)
                                .setType(dl.name())
                        ).setPublishTime(clock.instant().plus(dl.duration()).toEpochMilli())
                        .build())
                .build();
    }

    private EventDataInput toAppendOp(
            EventDataInput.Builder builder,
            Object data,
            Supplier<Integer> versionSupplier) {
        return builder
                .setType(eventTypeRegistry.getTypeNameForClass(data.getClass()))
                .setData(ByteString.copyFrom(codec.encode(data)))
                .setId(idGenerator.generate().toString())
                .setRevision(versionSupplier.get())
                .build();
    }

}
