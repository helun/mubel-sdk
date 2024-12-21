package io.mubel.sdk;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

/**
 * Represents the result of a command handler.
 *
 * @param events          The events that will be published.
 * @param deadlines       The deadlines that will be stored for the future.
 * @param scheduledEvents The events that will be published in the future.
 * @param cancelIds       The id's of the deadlines or schelued events that will be cancelled.
 * @param <T>
 */
public record HandlerResult<T>(
        List<T> events,
        List<Deadline> deadlines,
        List<ScheduledEvent<T>> scheduledEvents,
        Set<String> cancelIds
) {

    public HandlerResult {
        events = requireNonNullElse(events, List.of());
        deadlines = requireNonNullElse(deadlines, List.of());
        scheduledEvents = requireNonNullElse(scheduledEvents, List.of());
        cancelIds = requireNonNullElse(cancelIds, Set.of());
    }

    public static <T> HandlerResult.Builder<T> of(T event) {
        return new Builder<T>().events(List.of(event));
    }

    public static <T> HandlerResult.Builder<T> of(List<T> events) {
        return new Builder<T>().events(events);
    }

    public static <T> HandlerResult<T> empty() {
        return new HandlerResult<>(List.of(), List.of(), List.of(), Set.of());
    }

    public static <T> HandlerResult.Builder<T> builder() {
        return new Builder<>();
    }

    public void events(Consumer<List<T>> consumer) {
        if (!events.isEmpty()) {
            consumer.accept(events);
        }
    }

    public void deadlines(Consumer<List<Deadline>> consumer) {
        if (!deadlines.isEmpty()) {
            consumer.accept(deadlines);
        }
    }

    public void scheduledEvents(Consumer<List<ScheduledEvent<T>>> consumer) {
        if (!scheduledEvents.isEmpty()) {
            consumer.accept(scheduledEvents);
        }
    }

    public void cancelIds(Consumer<Collection<String>> consumer) {
        if (!cancelIds.isEmpty()) {
            consumer.accept(cancelIds);
        }
    }

    /**
     * Represents an event that will be published in the future.
     *
     * @param event    The event that will be published.
     * @param duration The duration after which the event will be published.
     * @param <T>
     */
    public record ScheduledEvent<T>(
            T event,
            Duration duration
    ) {
        public ScheduledEvent {
            event = requireNonNull(event);
            duration = requireNonNull(duration);
        }
    }

    public boolean isEmpty() {
        return events.isEmpty()
                && deadlines.isEmpty()
                && scheduledEvents.isEmpty()
                && cancelIds.isEmpty();
    }

    public static class Builder<T> {
        private List<T> events;
        private List<Deadline> deadlines;
        private List<ScheduledEvent<T>> scheduledEvents;
        private Set<String> cancelIds;

        /**
         * Publish an event.
         *
         * @param events
         * @return
         */
        public Builder<T> events(List<T> events) {
            this.events = events;
            return this;
        }

        /**
         * Schedule a deadline to be published in the future.
         *
         * @param deadline
         * @return
         */
        public Builder<T> deadline(Deadline deadline) {
            if (deadlines == null) {
                deadlines = new ArrayList<>(2);
            }
            if (!deadlines.isEmpty()) {
                deadlines.stream()
                        .filter(dl -> dl.name().equals(deadline.name()))
                        .findFirst()
                        .ifPresent(dl -> {
                            throw new IllegalArgumentException("Duplicate deadline name: " + dl.name());
                        });
            }
            deadlines.add(deadline);
            return this;
        }

        /**
         * Schedule an event to be published in the future.
         *
         * @param duration
         * @param event
         * @return
         */
        public Builder<T> schedule(Duration duration, T event) {
            if (scheduledEvents == null) {
                scheduledEvents = new ArrayList<>(2);
            }
            scheduledEvents.add(new ScheduledEvent<>(event, duration));
            return this;
        }

        /**
         * Cancel a deadline or scheduled event.
         *
         * @param id
         * @return
         */
        public Builder<T> cancel(String id) {
            if (cancelIds == null) {
                cancelIds = new HashSet<>(2);
            }
            cancelIds.add(id);
            return this;
        }

        public HandlerResult<T> build() {
            return new HandlerResult<>(
                    events,
                    deadlines,
                    scheduledEvents,
                    cancelIds
            );
        }
    }

}
