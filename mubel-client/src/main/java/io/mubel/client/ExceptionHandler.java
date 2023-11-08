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
            var pd = metadata.get(ProtoUtils.keyForProto(ProblemDetail.getDefaultInstance()));
            System.err.println(pd);
            return sre;
        } else {
            err.printStackTrace(System.err);
        }
        return new RuntimeException(cause);
    }

}
