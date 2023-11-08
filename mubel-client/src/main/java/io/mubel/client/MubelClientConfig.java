package io.mubel.client;

public record MubelClientConfig(
        String host,
        int port
) {
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String host;
        private int port;

        public Builder host(String s) {
            this.host = s;
            return this;
        }

        public Builder port(int p) {
            this.port = p;
            return this;
        }

        public MubelClientConfig build() {
            return new MubelClientConfig(host, port);
        }
    }
}
