package io.mubel.sdk.execution;

import io.mubel.sdk.eventstore.EventStore;
import io.mubel.sdk.execution.internal.InvocationContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IntegratedTest {
    String streamId = "streamId";

    @Mock
    EventStore eventStore;

    void test() {
        var ctx = InvocationContext.create(streamId);
    }


}
