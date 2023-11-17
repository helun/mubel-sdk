package io.mubel.sdk.scheduled;

import java.util.Set;
import java.util.function.Consumer;

public record ScheduledEventsConfig<T>(
        Consumer<T> consumer,
        Class<T> eventBaseClass,
        Set<String> categories
) {

    public static <T> ScheduledEventsConfig<T> forAllCategories(
            Consumer<T> consumer,
            Class<T> eventBaseClass
    ) {
        return new ScheduledEventsConfig<>(
                consumer,
                eventBaseClass,
                Set.of()
        );
    }

    public static <T> ScheduledEventsConfig<T> forCategories(
            Consumer<T> consumer,
            Class<T> eventBaseClass,
            String... categories) {
        return new ScheduledEventsConfig<>(
                consumer,
                eventBaseClass,
                Set.of(categories)
        );
    }

}
