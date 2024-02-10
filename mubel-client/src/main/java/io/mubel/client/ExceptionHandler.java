package io.mubel.client;

import com.google.common.base.Throwables;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.mubel.api.grpc.ProblemDetail;
import io.mubel.client.exceptions.BadRequestException;
import io.mubel.client.exceptions.ConnectionClosedException;
import io.mubel.client.exceptions.MubelClientException;
import io.mubel.client.exceptions.ServerException;

public class ExceptionHandler {

    public static RuntimeException handleFailure(Throwable err) {
        if (err instanceof MubelClientException mce) {
            return mce;
        }
        if (err == null) {
            throw new RuntimeException("unknown failure");
        }
        var cause = Throwables.getRootCause(err);
        if (cause instanceof io.grpc.StatusRuntimeException sre) {
            var status = sre.getStatus();
            System.err.println(status.getCode() + " " + status.getDescription());
            var metadata = Status.trailersFromThrowable(sre);
            ProblemDetail pd;
            if (metadata != null) {
                pd = metadata.get(ProtoUtils.keyForProto(ProblemDetail.getDefaultInstance()));
                System.err.println(pd);
            }
            if (status == Status.CANCELLED) {
                return new ConnectionClosedException(status.getDescription());
            }
            if (status == Status.INVALID_ARGUMENT) {
                return new BadRequestException(status.getDescription());
            }
            if (status == Status.INTERNAL) {
                return new ServerException(status.getDescription());
            }
        } else {
            err.printStackTrace(System.err);
        }
        return new MubelClientException(cause);
    }

    public static RuntimeException mapProblem(ProblemDetail detail) {
        var status = resolveStatus(detail.getStatus());
        switch (status.getCode()) {
            case Status.Code.CANCELLED:
                return new ConnectionClosedException(detail.getDetail());
            case Status.Code.INVALID_ARGUMENT:
                return new BadRequestException(detail.getDetail());
            case Status.Code.INTERNAL:
                return new ServerException(detail.getDetail());
            default:
                return new MubelClientException(detail.getDetail());
        }
    }

    private static Status resolveStatus(int inputCode) {
        for (var code : Status.Code.values()) {
            if (code.value() == inputCode) {
                return Status.fromCodeValue(inputCode);
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + inputCode);
    }

}
