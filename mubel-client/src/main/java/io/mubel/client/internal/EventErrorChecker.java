package io.mubel.client.internal;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.mubel.api.grpc.EventData;
import io.mubel.api.grpc.ProblemDetail;
import io.mubel.api.grpc.TriggeredEvents;
import io.mubel.client.MubelClientException;

import java.util.function.Function;

public class EventErrorChecker<T> implements Function<T, T> {

    private static final String MUBEL_PROBLEM_EVENT_TYPE = "__MBL_PROBLEM_EVENT__";
    private static final EventErrorChecker<EventData> EVENT_DATA_EVENT_ERROR_CHECKER = new EventErrorChecker<>(EventData::getType, EventData::getData);
    private static final EventErrorChecker<TriggeredEvents> SCHEDULED_EVENT_ERROR_CHECKER = new EventErrorChecker<>(
            te -> te.getEventCount() > 0 ? te.getEventList().getFirst().getType() : "",
            te -> te.getEventList().getFirst().getData()
    );
    private final Function<T, String> typeExtractor;
    private final Function<T, ByteString> dataExtractor;

    public static EventErrorChecker<EventData> eventDataEventErrorChecker() {
        return EVENT_DATA_EVENT_ERROR_CHECKER;
    }

    public static EventErrorChecker<TriggeredEvents> scheduledEventErrorChecker() {
        return SCHEDULED_EVENT_ERROR_CHECKER;
    }

    private EventErrorChecker(Function<T, String> typeExtractor, Function<T, ByteString> dataExtractor) {
        this.typeExtractor = typeExtractor;
        this.dataExtractor = dataExtractor;
    }

    @Override
    public T apply(T t) {
        if (isProblemType(typeExtractor.apply(t))) {
            throw new MubelClientException(parseProblemDetail(dataExtractor.apply(t)));
        }
        return t;
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
