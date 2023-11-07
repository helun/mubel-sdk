package io.mubel.sdk.execution;

import java.util.function.Consumer;

@FunctionalInterface
public interface EventDispatcher<T, E> {

    Consumer<E> resolveEventHandler(T eventHandlerInstance);

}
