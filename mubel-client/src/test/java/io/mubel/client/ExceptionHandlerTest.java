package io.mubel.client;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.mubel.client.exceptions.BadRequestException;
import io.mubel.client.exceptions.ConnectionClosedException;
import io.mubel.client.exceptions.ServerException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ExceptionHandlerTest {

    @Test
    void Status_CANCELLED_is_mapped_to_ConnectionClosedExeption() {
        var sre = new StatusRuntimeException(io.grpc.Status.CANCELLED);
        assertThat(ExceptionHandler.handleFailure(sre))
                .isInstanceOf(ConnectionClosedException.class);
    }

    @Test
    void Status_INVALID_ARGUMENT_is_mapped_to_BadRequestException() {
        var sre = new StatusRuntimeException(Status.INVALID_ARGUMENT);
        assertThat(ExceptionHandler.handleFailure(sre))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void Status_INTERNAL_is_mapped_to_ServerException() {
        var sre = new StatusRuntimeException(Status.INTERNAL);
        assertThat(ExceptionHandler.handleFailure(sre))
                .isInstanceOf(ServerException.class);
    }
}