package io.mubel.client.exceptions;

import io.mubel.api.grpc.v1.common.ProblemDetail;

import java.util.Optional;

public class BadRequestException extends MubelClientException {

    private final ProblemDetail problemDetail;

    public BadRequestException(String message, ProblemDetail problemDetail) {
        super(message);
        this.problemDetail = problemDetail;
    }

    public BadRequestException(String message) {
        super(message);
        this.problemDetail = null;
    }

    public Optional<ProblemDetail> problemDetail() {
        return Optional.ofNullable(problemDetail);
    }
}
