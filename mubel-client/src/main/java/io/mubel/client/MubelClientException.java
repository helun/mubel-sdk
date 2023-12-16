package io.mubel.client;

import io.mubel.api.grpc.ProblemDetail;

public class MubelClientException extends RuntimeException {

    private final ProblemDetail problemDetail;

    public MubelClientException(Throwable cause) {
        super(cause);
        problemDetail = null;
    }

    public MubelClientException(String message) {
        super(message);
        problemDetail = null;
    }

    public MubelClientException(String message, Throwable cause) {
        super(message, cause);
        problemDetail = null;
    }

    public MubelClientException(ProblemDetail problemDetail) {
        super(problemDetail.getDetail());
        this.problemDetail = problemDetail;
    }
}
