package io.mubel.sdk;

import com.google.protobuf.ByteString;
import io.mubel.api.grpc.EventData;
import io.mubel.api.grpc.EventDataInput;
import io.mubel.api.grpc.MetaData;
import io.mubel.api.grpc.ScheduledEvent;
import io.mubel.sdk.codec.EventDataCodec;
import io.mubel.sdk.internal.Constants;

import java.time.Clock;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.mubel.sdk.scheduled.ScheduleTimeCalculator.calculatePublishTime;
import static java.util.Objects.requireNonNull;

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

    public Object fromEventData(EventData eventData) {
        final var eventClass = eventTypeRegistry.getClassForType(eventData.getType());
        return codec.decode(eventData.getData().toByteArray(), eventClass);
    }

    public Object fromScheduledEvent(ScheduledEvent event) {
        final var eventClass = eventTypeRegistry.getClassForType(event.getType());
        return codec.decode(event.getData().toByteArray(), eventClass);
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

    public List<EventDataInput> toEventDataInput(String streamId, List<?> events, Supplier<Integer> versionSupplier) {
        final var builder = EventDataInput.newBuilder()
                .setStreamId(requireNonNull(streamId, "streamId may not be null"));
        return events.stream().map(e -> toEventDataInput(builder, e, versionSupplier)).toList();
    }

    public List<ScheduledEvent> toScheduledEvent(String streamId, String targetType, HandlerResult<?> input) {
        if (input.deadlines().isEmpty() && input.scheduledEvents().isEmpty()) {
            return List.of();
        }
        Stream<ScheduledEvent> result = null;
        if (!input.deadlines().isEmpty()) {
            result = input.deadlines().stream()
                    .map(d -> toScheduledEvent(streamId, targetType, d));
        }
        if (!input.scheduledEvents().isEmpty()) {
            final var seStream = input.scheduledEvents().stream()
                    .map(e -> toScheduledEvent(streamId, targetType, e));
            result = result != null ? Stream.concat(result, seStream) : seStream;
        }
        return result.toList();
    }

    private EventDataInput toEventDataInput(
            EventDataInput.Builder builder,
            Object data,
            Supplier<Integer> versionSupplier) {
        return builder
                .setType(eventTypeRegistry.getTypeNameForClass(data.getClass()))
                .setData(ByteString.copyFrom(codec.encode(data)))
                .setId(idGenerator.generate().toString())
                .setVersion(versionSupplier.get())
                .build();
    }

    private ScheduledEvent toScheduledEvent(String streamId, String targetType, Deadline deadline) {
        return ScheduledEvent.newBuilder()
                .setTargetEntityId(streamId)
                .setTargetType(targetType)
                .setCategory(Constants.DEADLINE_CATEGORY_NAME)
                .setDeadline(true)
                .setPublishTime(calculatePublishTime(deadline.duration(), clock))
                .setMetaData(MetaData.newBuilder()
                        .putData(Constants.DEADLINE_NAME_METADATA_KEY, deadline.name())
                        .build())
                .build();
    }

    private ScheduledEvent toScheduledEvent(String streamId, String targetType, HandlerResult.ScheduledEvent<?> event) {
        return ScheduledEvent.newBuilder()
                .setStreamId(streamId)
                .setTargetType(targetType)
                .setCategory(Constants.SCHEDULED_CATEGORY_NAME)
                .setPublishTime(calculatePublishTime(event.duration(), clock))
                .setData(ByteString.copyFrom(codec.encode(event.event())))
                .setType(eventTypeRegistry.getTypeNameForClass(event.event().getClass()))
                .build();
    }
}
