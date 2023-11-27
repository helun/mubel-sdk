package io.mubel.fixtures;

import java.util.UUID;

public sealed interface TestEvents {

    record EventA(String value, int processedEventCount) implements TestEvents {
    }

    record EventB(String value, int processedEventCount) implements TestEvents {
    }

    record EventWithDeadlineRef(UUID deadlineId) implements TestEvents {
    }

    record DeadlineExpired(String value) implements TestEvents {
    }
}
