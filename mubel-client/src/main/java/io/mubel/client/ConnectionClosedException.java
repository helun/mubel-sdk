package io.mubel.client;

public class ConnectionClosedException extends MubelClientException {

    public ConnectionClosedException(String message) {
        super(message);
    }
}
