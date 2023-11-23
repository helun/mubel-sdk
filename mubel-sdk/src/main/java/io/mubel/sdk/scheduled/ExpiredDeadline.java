package io.mubel.sdk.scheduled;

import java.util.UUID;

import static io.mubel.sdk.annotation.DeadlineHandler.DEFAULT_DEADLINE_NAME;

public record ExpiredDeadline(
        UUID targetEntityId,
        String deadlineName
) {

    public ExpiredDeadline {
        if (targetEntityId == null) {
            throw new IllegalArgumentException("targetEntityId cannot be null");
        }
        deadlineName = deadlineName == null || deadlineName.isEmpty() ? DEFAULT_DEADLINE_NAME : deadlineName;
    }

}
