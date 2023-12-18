package io.mubel.client.internal;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.mubel.api.grpc.EventData;
import io.mubel.api.grpc.ProblemDetail;
import io.mubel.api.grpc.TriggeredEvents;
import io.mubel.client.MubelClientException;

import java.util.function.Function;
import java.util.function.Predicate;

public class MubelSystemEventFilter<T> implements Predicate<T> {

    private static final String MUBEL_SYSTEM_EVENT_TYPE_PREFIX = "__MBL_";
    private static final String MUBEL_PROBLEM_EVENT_TYPE = "__MBL_PROBLEM_EVENT__";
    private static final MubelSystemEventFilter<EventData> EVENT_DATA_EVENT_ERROR_CHECKER = new MubelSystemEventFilter<>(EventData::getType, EventData::getData);
    private static final MubelSystemEventFilter<TriggeredEvents> SCHEDULED_EVENT_ERROR_CHECKER = new MubelSystemEventFilter<>(
            te -> te.getEventCount() > 0 ? te.getEventList().getFirst().getType() : "",
            te -> te.getEventList().getFirst().getData()
    );
    private final Function<T, String> typeExtractor;
    private final Function<T, ByteString> dataExtractor;

    public static MubelSystemEventFilter<EventData> eventDataEventErrorChecker() {
        return EVENT_DATA_EVENT_ERROR_CHECKER;
    }

    public static MubelSystemEventFilter<TriggeredEvents> scheduledEventErrorChecker() {
        return SCHEDULED_EVENT_ERROR_CHECKER;
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
