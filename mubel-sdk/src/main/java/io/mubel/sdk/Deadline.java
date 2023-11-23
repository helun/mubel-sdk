package io.mubel.sdk;

import java.time.Duration;
import java.util.UUID;

import static io.mubel.sdk.Constrains.safe;
import static java.util.Objects.requireNonNull;

/**
 * Represents a deadline that will be published unless it is cancelled.
 *
 * @param id       the id of the deadline, refer to this id to cancel the deadline.
 * @param name     the name of the deadline, this is used to distinguish between deadlines for the same target entity.
 * @param duration the duration after which the deadline will be published.
 */
public record Deadline(
        UUID id,
        String name,
        Duration duration
) {
    public Deadline {
        id = requireNonNull(id);
        name = safe(name);
        if (requireNonNull(duration).isNegative()) {
            throw new IllegalArgumentException("Duration must be positive");
        }
    }

    public Deadline(String name, Duration duration) {
        this(IdGenerator.timebasedGenerator().generate(), name, duration);
    }

    public static Deadline of(String name, Duration duration) {
        return new Deadline(name, duration);
    }

    public static Deadline ofSeconds(String name, int seconds) {
        return new Deadline(name, Duration.ofSeconds(seconds));
    }

    public static Deadline ofMinutes(String name, int minutes) {
        return new Deadline(name, Duration.ofMinutes(minutes));
    }

    public static Deadline ofHours(String name, int hours) {
        return new Deadline(name, Duration.ofHours(hours));
    }

    public static Deadline ofDays(String name, int days) {
        return new Deadline(name, Duration.ofDays(days));
    }

}
