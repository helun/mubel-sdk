package io.mubel.sdk.subscription;

import io.mubel.sdk.exceptions.MubelException;
import io.mubel.sdk.internal.Utils;

/**
 * @param consumerGroup  This is the consumer group that this state belongs to
 * @param sequenceNumber This is the sequence number of the last event that was processed
 * @param version        This is used for optimistic locking
 */
public record SubscriptionState(
        String consumerGroup,
        long sequenceNumber,
        int version
) {
    private static final int INITIAL_VERSION = -1;
    public static final int FIRST_VERSION = 0;
    private static final int INITIAL_SEQUENCE_NO = 0;

    public SubscriptionState {
        Utils.requireNonNull(consumerGroup, () -> new MubelException("consumerGroup may not be null or empty"));
        Utils.assertGteZeroLong(sequenceNumber, value -> new MubelException("sequenceNumber must be >= 0. was: %s".formatted(value)));
        if (version < INITIAL_VERSION) {
            throw new MubelException("version must be >= %d. was: %d".formatted(INITIAL_VERSION, version));
        }
    }

    public static SubscriptionState initialState(String consumerGroup) {
        return new SubscriptionState(consumerGroup, INITIAL_SEQUENCE_NO, INITIAL_VERSION);
    }

    public static SubscriptionState initialState(String consumerGroup, long startSequenceNumber) {
        return new SubscriptionState(consumerGroup, startSequenceNumber, INITIAL_VERSION);
    }

    public SubscriptionState withSequenceNumber(long sequenceNumber) {
        return new SubscriptionState(consumerGroup, sequenceNumber, version + 1);
    }

}
