package io.mubel.sdk;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.mubel.sdk.Constrains.safe;
import static java.util.Objects.requireNonNull;

/**
 * Represents a deadline that will be published unless it is cancelled.
 *
 * @param id       the id of the deadline, refer to this id to cancel the deadline.
 * @param name     the name of the deadline, this is used to distinguish between deadlines for the same target entity.
 * @param duration the duration after which the deadline will be published.
 * @param attributes the attributes of the deadline.
 */
public record Deadline(
        UUID id,
        String name,
        Duration duration,
        Map<String, String> attributes
) {
    public Deadline {
        id = requireNonNull(id);
        name = safe(name, name);
        if (requireNonNull(duration).isNegative()) {
            throw new IllegalArgumentException("Duration must be positive");
        }
    }

    public Deadline(String name, Duration duration) {
        this(IdGenerator.timebasedGenerator().generate(), name, duration, null);
    }

    public Deadline(String name, Duration duration, Map<String, String> attributes) {
        this(IdGenerator.timebasedGenerator().generate(), name, duration, attributes);
    }

    public static Deadline.Builder of(String name, Duration duration) {
        return new Deadline.Builder()
                .name(name)
                .duration(duration);
    }

    public static Deadline ofSeconds(String name, int seconds) {
        return new Deadline(name, Duration.ofSeconds(seconds));
    }

    public static Deadline.Builder ofMinutes(String name, int minutes) {
        return new Deadline.Builder()
                .name(name)
                .duration(Duration.ofMinutes(minutes));
    }

    public static Deadline.Builder ofHours(String name, int hours) {
        return new Deadline.Builder()
                .name(name)
                .duration(Duration.ofHours(hours));
    }

    public static Deadline.Builder ofDays(String name, int days) {
        return new Deadline.Builder()
                .name(name)
                .duration(Duration.ofDays(days));
    }

    public static Deadline.Builder builder() {
        return new Deadline.Builder();
    }

    public static class Builder {
        private String name;
        private Duration duration;
        private Map<String, String> attributes;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder attribute(String key, String value) {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            this.attributes.put(key, value);
            return this;
        }

        public Deadline build() {
            attributes = attributes != null ? Collections.unmodifiableMap(attributes) : Map.of();
            return new Deadline(name, duration, attributes);
        }

    }

}
