package io.mubel.sdk.subscription;

import java.util.Optional;

/**
 * A repository for subscription states.
 *
 * This interface is used to store and retrieve subscription states.
 */
public interface SubscriptionStateRepository {

    Optional<SubscriptionState> find(String consumerGroup);

    void put(SubscriptionState subscriptionState);

}
