package io.mubel.sdk.exceptions;

public class HandlerNotFoundException extends MubelException {

    public HandlerNotFoundException(String message) {
        super(message);
    }

    public static HandlerNotFoundException forCommand(String message) {
        return new HandlerNotFoundException(message + ".\n\tCheck that the handler method is public, annotated with @CommandHandler and returns HandlerResult, List or a single event");
    }

    public static HandlerNotFoundException forEvent(String message) {
        return new HandlerNotFoundException(message + ".\n\tCheck that the handler method is public, annotated with @EventHandler and returns void");
    }

    public static HandlerNotFoundException forDeadline(String message) {
        return new HandlerNotFoundException(message + ".\n\tCheck that the handler method is public, annotated with @DeadlineHandler and returns HandlerResult, List or a single event");
    }
}
