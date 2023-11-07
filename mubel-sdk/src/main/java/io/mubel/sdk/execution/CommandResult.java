package io.mubel.sdk.execution;

import java.util.UUID;

public record CommandResult(
        UUID streamId,
        int newEventCount,
        int oldVersion,
        int newVersion
) {

}
