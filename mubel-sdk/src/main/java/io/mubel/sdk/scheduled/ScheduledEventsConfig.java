package io.mubel.sdk.scheduled;

import java.util.Set;

public record ScheduledEventsConfig(
        Set<String> categories
) {

    public static ScheduledEventsConfig forAllCategories() {
        return new ScheduledEventsConfig(
                Set.of()
        );
    }

    public static <T> ScheduledEventsConfig forCategories(
            String... categories) {
        return new ScheduledEventsConfig(
                Set.of(categories)
        );
    }

}
