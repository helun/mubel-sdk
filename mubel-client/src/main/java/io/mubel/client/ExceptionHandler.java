package io.mubel.client;

import com.google.common.base.Throwables;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.mubel.api.grpc.ProblemDetail;

public class ExceptionHandler {

    public static RuntimeException handleFailure(Throwable err) {
        if (err == null) {
            throw new RuntimeException("unknown failure");
        }
        var cause = Throwables.getRootCause(err);
        if (cause instanceof io.grpc.StatusRuntimeException sre) {
            var status = sre.getStatus();
            System.err.println(status.getCode() + " " + status.getDescription());
            var metadata = Status.trailersFromThrowable(sre);
            if (metadata != null) {
                var pd = metadata.get(ProtoUtils.keyForProto(ProblemDetail.getDefaultInstance()));
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

}
