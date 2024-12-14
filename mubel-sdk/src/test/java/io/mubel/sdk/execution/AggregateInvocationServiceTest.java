package io.mubel.sdk.execution;

import com.google.protobuf.ByteString;
import io.mubel.api.grpc.v1.events.EventData;
import io.mubel.api.grpc.v1.events.ExecuteRequestOrBuilder;
import io.mubel.client.exceptions.MubelClientException;
import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.EventNamingStrategy;
import io.mubel.sdk.EventTypeRegistry;
import io.mubel.sdk.IdGenerator;
import io.mubel.sdk.codec.JacksonJsonEventDataCodec;
import io.mubel.sdk.eventstore.EventStore;
import io.mubel.sdk.exceptions.EventStreamNotFoundException;
import io.mubel.sdk.exceptions.MubelExecutionException;
import io.mubel.sdk.fixtures.TestAggregate;
import io.mubel.sdk.fixtures.TestCommands;
import io.mubel.sdk.fixtures.TestEvents;
import io.mubel.sdk.scheduled.ExpiredDeadline;
import org.assertj.core.api.AbstractIntegerAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
        verify(eventStore).execute(ArgumentMatchers.any(ExecuteRequestOrBuilder.class));
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
        verify(eventStore).execute(ArgumentMatchers.any(ExecuteRequestOrBuilder.class));
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

    @Test
    void handleDeadline() {
        final var existing = setupExistingStream();
        final var service = getService();
        service.deadlineExpired(new ExpiredDeadline(UUID.fromString(existing.getStreamId()), "", Map.of(), Instant.now()));
        verify(eventStore).execute(ArgumentMatchers.any(ExecuteRequestOrBuilder.class));
    }

    @Test
    void submit_fails() {
        setupFailingStream();
        final var service = getService();
        assertThatThrownBy(() -> service.submit(UUID.randomUUID(), new TestCommands.CommandA("value")))
                .isInstanceOf(MubelExecutionException.class);
    }

    @Test
    void findState_fails() {
        setupFailingStream();
        final var service = getService();
        assertThatThrownBy(() -> service.findState(UUID.randomUUID()))
                .isInstanceOf(MubelExecutionException.class);
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
                        .setRevision(i)
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
        //when(eventStore.get(ArgumentMatchers.anyString()))
        //      .thenReturn(events);
        when(eventStore.getAsync(ArgumentMatchers.anyString()))
                .thenReturn(Flux.fromIterable(events));
    }

    private void setupExistingStream(List<EventData> events, int toVersion) {
        //when(eventStore.get(ArgumentMatchers.anyString(), ArgumentMatchers.eq(toVersion)))
        //.thenReturn(events);
        when(eventStore.getAsync(ArgumentMatchers.anyString(), ArgumentMatchers.eq(toVersion)))
                .thenReturn(Flux.fromIterable(events));
    }

    private void setupNonExistingStream() {
        //when(eventStore.get(ArgumentMatchers.anyString())).thenReturn(List.of());
        when(eventStore.getAsync(ArgumentMatchers.anyString())).thenReturn(Flux.empty());
    }

    private void setupFailingStream() {
        //when(eventStore.get(ArgumentMatchers.anyString())).thenReturn(List.of());
        when(eventStore.getAsync(ArgumentMatchers.anyString())).thenReturn(Flux.error(new MubelClientException("some error")));
    }
}