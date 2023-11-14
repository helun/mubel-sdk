package io.mubel.sdk.execution;

import com.google.protobuf.ByteString;
import io.mubel.api.grpc.EventData;
import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.EventNamingStrategy;
import io.mubel.sdk.EventTypeRegistry;
import io.mubel.sdk.IdGenerator;
import io.mubel.sdk.codec.JacksonJsonEventDataCodec;
import io.mubel.sdk.eventstore.EventStore;
import io.mubel.sdk.exceptions.EventStreamNotFoundException;
import io.mubel.sdk.fixtures.TestAggregate;
import io.mubel.sdk.fixtures.TestCommands;
import io.mubel.sdk.fixtures.TestEvents;
import org.assertj.core.api.AbstractIntegerAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregateInvocationServiceTest {

    @Mock
    EventStore eventStore;

    EventDataMapper eventDataMapper = new EventDataMapper(
            new JacksonJsonEventDataCodec(),
            EventTypeRegistry.builder()
                    .withNamingStrategy(EventNamingStrategy.byClass())
                    .build(),
            IdGenerator.timebasedGenerator()
    );

    @Test
    void submit() {
        setupExistingStream();

        final var service = getService();
        final var result = service.submit(UUID.randomUUID(), new TestCommands.CommandA("value"));
        assertThat(result.newEventCount()).isEqualTo(1);
        assertThat(result.newVersion()).isEqualTo(1);
        assertThat(result.oldVersion()).isEqualTo(0);
        assertThat(result.newEvents())
                .hasSize(1)
                .first()
                .satisfies(event -> assertThat(event).isInstanceOf(TestEvents.EventA.class));
        verify(eventStore).append(ArgumentMatchers.anyList());
    }

    private TestAggregateInvocationService getService() {
        return new TestAggregateInvocationService(eventStore, eventDataMapper);
    }

    @Test
    void submitWithNoExistingEvents() {
        setupNonExistingStream();
        final var service = getService();
        final var result = service.submit(UUID.randomUUID(), new TestCommands.CommandA("value"));
        assertThat(result.newEventCount()).isEqualTo(1);
        assertThat(result.newVersion()).isEqualTo(0);
        assertThat(result.oldVersion()).isEqualTo(-1);
        verify(eventStore).append(ArgumentMatchers.anyList());
    }

    @Test
    void getState() {
        final var existing = setupExistingStream();
        final var service = getService();
        final var result = service.getState(UUID.fromString(existing.getStreamId()));
        assertThat(result).isNotNull();
        assertState(result);
    }

    @Test
    void getStateByVersion() {
        final var streamId = UUID.randomUUID();
        int toVersion = 1;
        int count = 3;
        setupExistingStream(events(streamId, count), toVersion);
        final var service = getService();
        final var result = service.getState(streamId, toVersion);
        assertThat(result).isNotNull();
        assertState(result, count);
    }

    @Test
    void getMissingState() {
        setupNonExistingStream();
        final var service = getService();
        assertThatThrownBy(() -> service.getState(UUID.randomUUID()))
                .as("should throw EventStreamNotFoundException")
                .isInstanceOf(EventStreamNotFoundException.class);
    }

    @Test
    void findState() {
        final var existing = setupExistingStream();
        final var service = getService();
        final var result = service.findState(UUID.fromString(existing.getStreamId()));
        assertThat(result)
                .hasValueSatisfying(AggregateInvocationServiceTest::assertState);
    }

    @Test
    void findMissingState() {
        setupNonExistingStream();
        final var service = getService();
        final var result = service.findState(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    private static AbstractIntegerAssert<?> assertState(TestAggregate aggregate) {
        return assertState(aggregate, 1);
    }

    private static AbstractIntegerAssert<?> assertState(TestAggregate aggregate, int expectedEventCount) {
        return assertThat(aggregate.getProcessedEventCount()).isEqualTo(expectedEventCount);
    }

    private static EventData event() {
        return events(UUID.randomUUID(), 1).getFirst();
    }

    private static List<EventData> events(UUID streamId, int count) {
        return IntStream.range(0, count).mapToObj(i -> EventData.newBuilder()
                        .setStreamId(streamId.toString())
                        .setType(TestEvents.EventA.class.getName())
                        .setData(eventData(count))
                        .setVersion(i)
                        .build())
                .toList();

    }

    private static ByteString eventData(int count) {
        try {
            return ByteString.copyFrom("""
                    {
                        "value": "value",
                        "processedEventCount": %d
                    }
                    """.formatted(count), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private EventData setupExistingStream() {
        final var existing = event();
        setupExistingStream(List.of(existing));
        return existing;
    }

    private void setupExistingStream(List<EventData> events) {
        when(eventStore.get(ArgumentMatchers.anyString()))
                .thenReturn(events);
    }

    private void setupExistingStream(List<EventData> events, int toVersion) {
        when(eventStore.get(ArgumentMatchers.anyString(), ArgumentMatchers.eq(toVersion)))
                .thenReturn(events);
    }

    private void setupNonExistingStream() {
        when(eventStore.get(ArgumentMatchers.anyString())).thenReturn(List.of());
    }
}