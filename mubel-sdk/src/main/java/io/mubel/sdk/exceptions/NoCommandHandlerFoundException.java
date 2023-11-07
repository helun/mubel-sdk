package io.mubel.sdk.exceptions;

public class NoCommandHandlerFoundException extends MubelException {

    public NoCommandHandlerFoundException(String message) {
        super(message + ".\n\tCheck that the handler method is public and returns a non null java.util.List with the expected event types.");
    }
}
