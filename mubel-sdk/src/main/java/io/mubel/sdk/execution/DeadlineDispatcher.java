package io.mubel.sdk.execution;

import io.mubel.sdk.HandlerResult;
import io.mubel.sdk.scheduled.ExpiredDeadline;

@FunctionalInterface
public interface DeadlineDispatcher<T, E> {

    HandlerResult<E> dispatch(T aggregateInstance, ExpiredDeadline deadline);

}
