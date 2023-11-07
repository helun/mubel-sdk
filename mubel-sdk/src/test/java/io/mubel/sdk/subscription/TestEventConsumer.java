package io.mubel.sdk.subscription;

import io.mubel.sdk.EventMessage;
import io.mubel.sdk.EventMessageBatch;
import io.mubel.sdk.EventMessageBatchConsumer;

import java.util.ArrayList;
import java.util.List;

public class TestEventConsumer<T> implements EventMessageBatchConsumer<T> {

    private final List<EventMessage<T>> events = new ArrayList<>();

    @Override
    public void accept(EventMessageBatch<T> t) {
        events.addAll(t.events());
    }

    public List<EventMessage<T>> getEvents() {
        return events;
    }
}
