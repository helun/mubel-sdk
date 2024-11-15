package io.mubel.sdk.fixtures;

import com.google.protobuf.ByteString;
import io.mubel.api.grpc.v1.events.EventData;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;

public final class Fixtures {

    private Fixtures() {
    }

    private static long sequenceNo = 6000;

    public static EventData.Builder eventDataBuilder() {
        return EventData.newBuilder()
                .setId(uuid())
                .setStreamId(uuid())
                .setType(TestEvents.EventA.class.getName())
                .setRevision(0)
                .setSequenceNo(sequenceNo++)
                .setCreatedAt(Clock.systemUTC().millis())
                .setData(ByteString.copyFrom("""
                        {
                           "value": "v",
                           "processedEventCount": 1
                        }
                        """, StandardCharsets.UTF_8));
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String esid() {
        return "abc:cde";
    }
}
