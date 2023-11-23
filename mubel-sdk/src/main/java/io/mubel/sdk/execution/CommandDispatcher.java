package io.mubel.sdk.execution;

import io.mubel.sdk.HandlerResult;

import java.util.function.Function;

@FunctionalInterface
public interface CommandDispatcher<T, E, C> {

    Function<C, HandlerResult<E>> resolveCommandHandler(T aggregateInstance);

}
