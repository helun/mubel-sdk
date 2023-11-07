package io.mubel.sdk.execution.internal;

import static java.util.Objects.requireNonNull;

public class InvocationContext {

    private final String streamId;
    private final AggregateVersion currentVersion;

    public static InvocationContext create(String streamId) {
        return new InvocationContext(streamId, AggregateVersion.initialVersion());
    }

    public InvocationContext(String streamId, AggregateVersion currentVersion) {
        this.streamId = requireNonNull(streamId);
        this.currentVersion = requireNonNull(currentVersion);
    }

    public String streamId() {
        return streamId;
    }

    public void applyVersion(int version) {
        currentVersion.applyVersion(version);
    }

    public int nextVersion() {
        return currentVersion.nextVersion();
    }

    public int currentVersion() {
        return currentVersion.get();
    }
}
