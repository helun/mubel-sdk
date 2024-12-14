package io.mubel.sdk.exceptions;

import java.util.UUID;

public class EventStreamNotFoundException extends MubelException {

    public EventStreamNotFoundException(UUID streamId) {
        super("stream id: %s not found".formatted(streamId));
    }
}
