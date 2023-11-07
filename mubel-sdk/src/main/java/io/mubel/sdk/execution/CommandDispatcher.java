package io.mubel.sdk.execution;

import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface CommandDispatcher<T, E, C> {

    Function<C, List<E>> resolveCommandHandler(T aggregateInstance);

}
