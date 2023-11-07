package io.mubel.sdk.subscription;

import java.util.Optional;

public interface SubscriptionStateRepository {

    Optional<SubscriptionState> find(String consumerGroup);

    void put(SubscriptionState subscriptionState);

}
