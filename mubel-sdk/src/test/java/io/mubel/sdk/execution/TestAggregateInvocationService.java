package io.mubel.sdk.execution;

import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.eventstore.EventStore;
import io.mubel.sdk.fixtures.TestAggregate;
import io.mubel.sdk.fixtures.TestCommands;
import io.mubel.sdk.fixtures.TestEvents;

public class TestAggregateInvocationService extends AggregateInvocationService<TestAggregate, TestEvents, TestCommands> {

    public TestAggregateInvocationService(EventStore eventStore, EventDataMapper eventDataMapper) {
        super(AggregateInvocationConfig.of(
                        TestAggregate::new,
                        a -> a::apply,
                        a -> a::handle
                ),
                eventStore,
                eventDataMapper
        );
    }
}
