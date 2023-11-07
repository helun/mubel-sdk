package io.mubel.sdk;

import java.util.Iterator;
import java.util.List;

public record EventMessageBatch<E>(
        List<EventMessage<E>> events,
        long lastSequenceNo
) implements Iterable<EventMessage<E>> {

    @Override
    public Iterator<EventMessage<E>> iterator() {
        return events.iterator();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public int size() {
        return events.size();
    }
}
