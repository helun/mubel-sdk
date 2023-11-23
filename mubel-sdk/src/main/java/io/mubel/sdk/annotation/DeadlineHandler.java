package io.mubel.sdk.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeadlineHandler {

    String DEFAULT_DEADLINE_NAME = "all";

    String value() default DEFAULT_DEADLINE_NAME;
}
