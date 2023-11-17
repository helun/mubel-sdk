package io.mubel.sdk;

import com.google.protobuf.ByteString;
import io.mubel.api.grpc.EventData;
import io.mubel.api.grpc.EventDataInput;
import io.mubel.api.grpc.ScheduledEvent;
import io.mubel.sdk.codec.EventDataCodec;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class EventDataMapper {

    private final EventDataCodec codec;
    private final EventTypeRegistry eventTypeRegistry;
    private final IdGenerator idGenerator;

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
}
