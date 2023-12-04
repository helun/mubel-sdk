package io.mubel.sdk.scheduled;

public interface ExpiredDeadlineConsumer {

    void deadlineExpired(ExpiredDeadline event);
    
    String targetType();

}
