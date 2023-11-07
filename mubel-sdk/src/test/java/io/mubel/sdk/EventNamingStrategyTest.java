package io.mubel.sdk;

import io.mubel.sdk.fixtures.TestEvents;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventNamingStrategyTest {

    @Test
    void bySimpleClassName() {
        final var strategy = EventNamingStrategy.byClassSimpleName(
                TestEvents.class
        );

        assertThat(strategy.nameFor(TestEvents.EventA.class))
                .isEqualTo("EventA");

        assertThat(strategy
                .nameFor(TestEvents.EventB.class))
                .isEqualTo("EventB");
    }

    @Test
    void byClassName() {
        assertThat(EventNamingStrategy.byClass()
                .nameFor(TestEvents.EventA.class))
                .isEqualTo("io.mubel.sdk.fixtures.TestEvents$EventA");

        assertThat(EventNamingStrategy.byClass()
                .classFor("io.mubel.sdk.fixtures.TestEvents$EventA"))
                .isEqualTo(TestEvents.EventA.class);
    }

    @Test
    void mappedEventNames() {
        final var strategy = EventNamingStrategy.mapped(Map.of(
                "MyEventA", TestEvents.EventA.class,
                "MyEventB", TestEvents.EventB.class
        ));

        assertThat(strategy.nameFor(TestEvents.EventA.class))
                .isEqualTo("MyEventA");

        assertThat(strategy.classFor("MyEventA"))
                .isEqualTo(TestEvents.EventA.class);

        assertThat(strategy.nameFor(TestEvents.EventB.class))
                .isEqualTo("MyEventB");
    }

}