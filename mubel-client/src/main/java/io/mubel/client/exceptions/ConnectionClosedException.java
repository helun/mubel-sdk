package io.mubel.client.exceptions;

public class ConnectionClosedException extends MubelClientException {

    public ConnectionClosedException(String message) {
        super(message);
    }
}
