package io.mubel.sdk;

import java.util.Iterator;
import java.util.List;

/**
 * A batch of event messages.
 *
 * This class is used to represent a batch of event messages.
 */
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
