package io.mubel.sdk;

import io.mubel.sdk.codec.JacksonJsonEventDataCodec;
import io.mubel.sdk.subscription.TestEventConsumer;

public final class TestComponents {

    private TestComponents() {
    }

    private static final IdGenerator idGenerator = IdGenerator.timebasedGenerator();

    public static IdGenerator idGenerator() {
        return idGenerator;
    }

    public static EventDataMapper eventDataMapper() {
        return new EventDataMapper(
                new JacksonJsonEventDataCodec(),
                EventTypeRegistry.builder()
                        .withNamingStrategy(EventNamingStrategy.byClass())
                        .build(),
                idGenerator);
    }

    public static <T> TestEventConsumer<T> testEventConsumer() {
        return new TestEventConsumer<>();
    }
}
