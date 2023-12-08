package io.mubel.client;

import io.mubel.api.grpc.EventData;

import java.util.List;

public interface Subscription {

    EventData next() throws InterruptedException;

    List<EventData> nextBatch(int size) throws InterruptedException;

}
