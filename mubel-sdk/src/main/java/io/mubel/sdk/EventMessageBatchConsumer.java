package io.mubel.sdk;

import java.util.function.Consumer;

public interface EventMessageBatchConsumer<T> extends Consumer<EventMessageBatch<T>> {
}
