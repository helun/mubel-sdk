package io.mubel.client;

public class MubelClientException extends RuntimeException {

    public MubelClientException(Throwable cause) {
        super(cause);
    }

    public MubelClientException(String message) {
        super(message);
    }

    public MubelClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
