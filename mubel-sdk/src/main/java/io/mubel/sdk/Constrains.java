package io.mubel.sdk;

import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;

import java.util.regex.Pattern;

public class Constrains {

    public static final String EVENT_TYPE_REGEXP = "^([A-Za-z0-9_-])+([\\.:/]?([A-Za-z0-9_-])*[\\$\\+]*([A-Za-z0-9_-])*)*$";
    public static final Pattern EVENT_TYPE_PTRN = Pattern.compile(EVENT_TYPE_REGEXP);
    public static final String ESID_REGEXP = EVENT_TYPE_REGEXP;
    public static final Pattern ESID_PTRN = EVENT_TYPE_PTRN;
    public static final Pattern PROPERTY_PATH_PTRN = Pattern.compile("^([A-Za-z0-9_-])+(\\.?([A-Za-z0-9_-])*)*$");
    public static final String SAFE_STRING_REGEXP = "[A-Za-z0-9_-]{1,255}";
    public static final Pattern SAFE_STRING_PTRN = Pattern.compile(SAFE_STRING_REGEXP);
    public static final String ADDRESS_REGEXP = "^(?:(?:[a-zA-Z0-9-]+\\.)*[a-zA-Z0-9-]+|(?:\\d{1,3}\\.){3}\\d{1,3})(?::\\d{1,5})?$";
    public static final Pattern ADDRESS_PTRN = Pattern.compile(ADDRESS_REGEXP);

    public static String validateEventStoreId(String s) {
        return Utils.validate(
                requireNonNull(s, "Event store id"),
                Constrains.ESID_PTRN,
                () -> new MubelConfigurationException("Event store id must match <namespace>:<event store name> pattern %s".formatted(Constrains.ESID_PTRN))
        );
    }

    public static <T> T requireNonNull(T value, String fieldName) {
        return Utils.requireNonNull(
                value,
                () -> new MubelConfigurationException(fieldName + " may not be null")
        );
    }

    public static String safe(String input, String fieldName) {
        return Utils.validate(
                requireNonNull(input, "Input"),
                Constrains.SAFE_STRING_PTRN,
                () -> new IllegalArgumentException("Invalid value for field: %s. Use 1-255 characters, including letters, numbers, hyphens, and underscores".formatted(fieldName))
        );
    }

}
