package io.mubel.test.internal;

import io.mubel.sdk.Deadline;
import io.mubel.sdk.HandlerResult;
import io.mubel.sdk.execution.AggregateInvocationConfig;
import io.mubel.sdk.scheduled.ExpiredDeadline;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class AggregateTestExecutor<T> {
    private Clock clock = Clock.systemUTC();
    private final List<HandlerResult<Object>> commandResults = new ArrayList<>();
    private final List<Object> recordedEvents = new ArrayList<>();
    private int verifyEventsFromIndex = 0;
    private final List<RecordedDeadline> recordedDeadlines = new ArrayList<>();
    private int verifyDeadlinesFromIndex = 0;
    private final Set<String> cancelledDeadlines = new HashSet<>();
    private final T state;
    private final AggregateInvocationConfig<T, Object, Object> config;

    public AggregateTestExecutor(AggregateInvocationConfig<T, Object, Object> config) {
        this.config = config;
        this.state = config.aggregateSupplier().get();
    }

    public void applyEvents(List<?> events) {
        final var dispatcher = config.eventDispatcher().resolveEventHandler(state);
        for (final var event : events) {
            dispatcher.accept(event);
        }
    }

    public void applyCommand(Object command) {
        applyCommandInternal(command);
    }

    public void applyAndRecordCommand(Object command) {
        final var result = applyCommandInternal(command);
        commandResults.add(result);
        recordedEvents.addAll(result.events());
    }

    private HandlerResult<Object> applyCommandInternal(Object command) {
        final var result = config.commandDispatcher()
                .resolveCommandHandler(state)
                .apply(command);
        applyEvents(result.events());
        recordDeadlines(result);
        return result;
    }

    private void recordDeadlines(HandlerResult<Object> result) {
        recordedDeadlines.addAll(mapToRecordedDeadline(result.deadlines()));
        cancelledDeadlines.addAll(result.cancelIds());
    }

    private Collection<RecordedDeadline> mapToRecordedDeadline(List<Deadline> deadlines) {
        return deadlines.stream()
                .map(deadline -> new RecordedDeadline(deadline, clock.instant()))
                .toList();
    }

    public void advanceTimeBy(Duration duration) {
        clock = Clock.offset(clock, duration);
        checkDeadlines();
    }

    private void checkDeadlines() {
        final var now = clock.instant();
        final var expiredDeadlines = recordedDeadlines.stream()
                .filter(recordedDeadline -> !cancelledDeadlines.contains(recordedDeadline.deadline.id().toString()))
                .filter(recordedDeadline -> recordedDeadline.isExpired(now))
                .toList();

        for (final var expiredDeadline : expiredDeadlines) {
            applyDeadline(expiredDeadline.deadline);
        }
        recordedDeadlines.removeAll(expiredDeadlines);
    }

    private void applyDeadline(Deadline expiredDeadline) {
        final var result = config.deadlineDispatcher().dispatch(state, mapExpiredDeadline(expiredDeadline));
        
        recordedEvents.addAll(result.events());
        commandResults.add(result);
        applyEvents(result.events());
        recordDeadlines(result);
    }

    private ExpiredDeadline mapExpiredDeadline(Deadline expiredDeadline) {
        return new ExpiredDeadline(
                expiredDeadline.id(),
                expiredDeadline.name(),
                expiredDeadline.attributes(),
                clock.instant());
    }

    public int executionCount() {
        return commandResults.size();
    }

    public T state() {
        return state;
    }

    public List<Object> events() {
        return recordedEvents.stream()
                .skip(verifyEventsFromIndex)
                .toList();
    }

    public List<Deadline> deadlines() {
        return recordedDeadlines.stream()
                .skip(verifyDeadlinesFromIndex)
                .map(RecordedDeadline::deadline)
                .toList();
    }

    public void setCheckPoint() {
        verifyEventsFromIndex = recordedEvents.size();
        verifyDeadlinesFromIndex = recordedDeadlines.size();
    }

    private record RecordedDeadline(Deadline deadline, Instant recordedAt) {

        boolean isExpired(Instant now) {
            final var expireTime = recordedAt.plus(deadline.duration());
            return expireTime.isBefore(now) || expireTime.equals(now);
        }

    }
}
