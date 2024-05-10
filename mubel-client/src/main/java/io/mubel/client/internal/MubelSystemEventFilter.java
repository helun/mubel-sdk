package io.mubel.client.internal;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.mubel.api.grpc.v1.common.ProblemDetail;
import io.mubel.api.grpc.v1.events.EventData;
import io.mubel.client.exceptions.MubelClientException;

import java.util.function.Function;
import java.util.function.Predicate;

public class MubelSystemEventFilter<T> implements Predicate<T> {

    private static final String MUBEL_SYSTEM_EVENT_TYPE_PREFIX = "__MBL_";
    private static final String MUBEL_PROBLEM_EVENT_TYPE = "__MBL_PROBLEM_EVENT__";
    private static final MubelSystemEventFilter<EventData> EVENT_DATA_EVENT_ERROR_CHECKER = new MubelSystemEventFilter<>(EventData::getType, EventData::getData);
    private final Function<T, String> typeExtractor;
    private final Function<T, ByteString> dataExtractor;

    public static MubelSystemEventFilter<EventData> eventDataEventErrorChecker() {
        return EVENT_DATA_EVENT_ERROR_CHECKER;
    }

    private MubelSystemEventFilter(Function<T, String> typeExtractor, Function<T, ByteString> dataExtractor) {
        this.typeExtractor = typeExtractor;
        this.dataExtractor = dataExtractor;
    }

    @Override
    public boolean test(T t) {
        final var type = typeExtractor.apply(t);
        if (!isSystemEvent(type)) {
            return true;
        }
        if (isProblemType(type)) {
            throw new MubelClientException(parseProblemDetail(dataExtractor.apply(t)));
        }
        return false;
    }

    private boolean isSystemEvent(String type) {
        return type.startsWith("__MBL_");
    }

    protected ProblemDetail parseProblemDetail(ByteString data) {
        try {
            return ProblemDetail.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new MubelClientException(e);
        }
    }

    protected boolean isProblemType(String type) {
        return MUBEL_PROBLEM_EVENT_TYPE.equals(type);
    }

}
