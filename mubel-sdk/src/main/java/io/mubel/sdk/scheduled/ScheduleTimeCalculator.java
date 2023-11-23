package io.mubel.sdk.scheduled;

import java.time.Clock;
import java.time.Duration;

public final class ScheduleTimeCalculator {

    private ScheduleTimeCalculator() {
    }

    public static long calculatePublishTime(Duration duration, Clock clock) {
        return clock.instant().plus(duration).toEpochMilli();
    }

}
