package io.mubel.sdk.execution.internal;

/**
 * Not thread safe
 */
public final class AggregateVersion {

    private AggregateVersion() {
    }

    private int version = -1;

    public static AggregateVersion initialVersion() {
        return new AggregateVersion();
    }

    public void assertInitialVersion() {
        assert version == -1;
    }

    public int nextVersion() {
        version++;
        return version;
    }

    public void applyVersion(int version) {
        assert version == this.version + 1;
        this.version = version;
    }

    public int get() {
        return version;
    }
}
