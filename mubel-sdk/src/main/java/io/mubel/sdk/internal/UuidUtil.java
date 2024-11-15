package io.mubel.sdk.internal;

import com.fasterxml.uuid.impl.UUIDUtil;

import java.util.UUID;

public final class UuidUtil {

    public static UUID parseUuid(String input) {
        return UUIDUtil.uuid(input);
    }

}
