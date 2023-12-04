package io.mubel.sdk.scheduled;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.mubel.sdk.annotation.DeadlineHandler.DEFAULT_DEADLINE_NAME;

public record ExpiredDeadline(
        UUID targetEntityId,
        String deadlineName,
        Map<String, String> attributes,
        Instant timestamp
) {

    public ExpiredDeadline {
        if (targetEntityId == null) {
            throw new IllegalArgumentException("targetEntityId cannot be null");
        }
        deadlineName = deadlineName == null || deadlineName.isEmpty() ? DEFAULT_DEADLINE_NAME : deadlineName;
        attributes = attributes == null ? Map.of() : attributes;
    }

    public String attribute(String key) {
        return attributes.get(key);
    }

    public Integer intAttribute(String key) {
        String v = attributes.get(key);
        return v != null ? Integer.parseInt(v) : null;
    }

    public Long longAttribute(String key) {
        String v = attributes.get(key);
        return v != null ? Long.parseLong(v) : null;
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

}
