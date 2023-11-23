package io.mubel.sdk.scheduled;

import java.util.function.Consumer;

public interface ExpiredDeadlineConsumer extends Consumer<ExpiredDeadline> {

    String targetType();

}
