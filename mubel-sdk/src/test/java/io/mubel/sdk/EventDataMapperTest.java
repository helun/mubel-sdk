package io.mubel.sdk;

import io.mubel.api.grpc.v1.events.EntityReference;
import io.mubel.api.grpc.v1.events.EventData;
import io.mubel.api.grpc.v1.events.Operation;
import io.mubel.sdk.execution.internal.InvocationContext;
import io.mubel.sdk.fixtures.TestEvents;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventDataMapperTest {

    EventDataMapper eventDataMapper = TestComponents.eventDataMapper();

    @Test
    void roundtrip() {
        final var ctx = InvocationContext.create("streamId");
        final var eventA = new TestEvents.EventA("a value", 0);
        final var operation = eventDataMapper.toAppendOp(ctx.streamId(), List.of(eventA), ctx::nextVersion);
        final var appendOp = operation.getAppend();
        assertThat(appendOp.getEventList())
                .hasSize(1)
                .first()
                .satisfies(ed -> {
                    assertThat(ed.getId()).isNotNull();
                    assertThat(ed.getType()).isEqualTo(TestEvents.EventA.class.getName());
                    assertThat(ed.getStreamId()).isEqualTo("streamId");
                    assertThat(ed.getData().toString("utf8")).isEqualTo("{\"value\":\"a value\",\"processedEventCount\":0}");
                });

        final var eventDataInput = appendOp.getEvent(0);
        final var eventData = EventData.newBuilder()
                .setId(eventDataInput.getId())
                .setType(eventDataInput.getType())
                .setStreamId(eventDataInput.getStreamId())
                .setRevision(eventDataInput.getRevision())
                .setData(eventDataInput.getData())
                .build();
        final var mappedEvent = eventDataMapper.fromEventData(eventData);
        assertThat(mappedEvent).isEqualTo(eventA);
    }

    @Test
    void deadline() {
        var deadline = Deadline.ofMinutes("a-deadline", 1)
                .attribute("key", "value")
                .build();

        var handlerResult = HandlerResult.of(List.of())
                .deadline(deadline)
                .build();

        UUID streamId = UUID.randomUUID();
        var reference = EntityReference.newBuilder()
                .setId(streamId.toString())
                .setType("some.target")
                .build();
        var deadlineOps = eventDataMapper.toDeadlineOps(reference, handlerResult.deadlines());
        assertThat(deadlineOps)
                .hasSize(1)
                .map(Operation::getScheduleDeadline)
                .first()
                .satisfies(se -> {
                    assertThat(se.getDeadline().getTargetEntity()).isEqualTo(reference);
                    assertThat(se.getDeadline().getData().toString(Charset.defaultCharset())).isEqualTo("{\"key\":\"value\"}");
                });

        var expiredDeadline = eventDataMapper.mapExpiredDeadline(deadlineOps.getFirst().getScheduleDeadline().getDeadline(), Instant.now());
        assertThat(expiredDeadline.targetEntityId()).isEqualTo(streamId);
        assertThat(expiredDeadline.deadlineName()).isEqualTo(deadline.name());
        assertThat(expiredDeadline.hasAttribute("key")).isTrue();
        assertThat(expiredDeadline.attribute("key")).isEqualTo("value");
    }

}