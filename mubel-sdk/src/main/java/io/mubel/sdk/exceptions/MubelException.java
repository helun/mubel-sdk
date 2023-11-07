package io.mubel.sdk.exceptions;

public class MubelException extends RuntimeException {

    public MubelException(String message) {
        super(message);
    }

    public MubelException(String message, Throwable cause) {
        super(message, cause);
    }

    public MubelException(Throwable cause) {
        super(cause);
    }

    public MubelException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
