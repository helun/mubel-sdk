package io.mubel.client;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHandlerTest {

    @Test
    void statusCancelled() {
        var sre = new StatusRuntimeException(io.grpc.Status.CANCELLED);
        assertThat(ExceptionHandler.handleFailure(sre))
                .isInstanceOf(ConnectionClosedException.class);
    }

    @Test
    void statusInvalidArgument() {
        var sre = new StatusRuntimeException(Status.INVALID_ARGUMENT);
        assertThat(ExceptionHandler.handleFailure(sre))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void statusInternal() {
        var sre = new StatusRuntimeException(Status.INTERNAL);
        assertThat(ExceptionHandler.handleFailure(sre))
                .isInstanceOf(ServerException.class);
    }
}