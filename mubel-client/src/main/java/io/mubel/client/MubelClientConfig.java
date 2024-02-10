package io.mubel.client;

import io.mubel.client.exceptions.MubelClientException;

import java.util.concurrent.Executor;

import static java.util.Objects.requireNonNull;

public record MubelClientConfig(
        String address,
        Executor executor
) {
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String address;
        private Executor executor;

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public MubelClientConfig build() {
            if (requireNonNull(address).isBlank()) {
                throw new MubelClientException("Address cannot be empty");
            }
            return new MubelClientConfig(
                    address,
                    executor
            );
        }
    }
}
