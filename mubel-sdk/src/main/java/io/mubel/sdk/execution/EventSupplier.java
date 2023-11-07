package io.mubel.sdk.execution;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@FunctionalInterface
public interface EventSupplier<E> extends Function<UUID, List<E>> {
}
