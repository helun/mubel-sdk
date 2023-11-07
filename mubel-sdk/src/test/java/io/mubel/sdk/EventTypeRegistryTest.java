package io.mubel.sdk;

import io.mubel.sdk.fixtures.TestEvents;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventTypeRegistryTest {

    @Test
    void byClassTypeNames() {
        final var registry = EventTypeRegistry.builder()
                .withNamingStrategy(EventNamingStrategy.byClass())
                .build();

        assertThat(registry.getTypeNameForClass(TestEvents.EventA.class))
                .isEqualTo("io.mubel.sdk.fixtures.TestEvents$EventA");

    }

}