package io.mubel.spring;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.containers.GenericContainer;

public class MubelContainerDetailsFactory extends ContainerConnectionDetailsFactory<GenericContainer<?>, MubelConnectionDetails> {

    public MubelContainerDetailsFactory() {
        super("mubel");
    }

    @Override
    protected MubelConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<GenericContainer<?>> source) {
        return new MubelContainerConnectionDetails(source);
    }

    static class MubelContainerConnectionDetails extends ContainerConnectionDetailsFactory.ContainerConnectionDetails<GenericContainer<?>>
            implements MubelConnectionDetails {

        @Override
        public String getAddress() {
            final var container = this.getContainer();
            final int port = container.getFirstMappedPort();
            final var host = container.getHost();
            return "%s:%d".formatted(host, port);
        }

        public MubelContainerConnectionDetails(ContainerConnectionSource<GenericContainer<?>> source) {
            super(source);
        }

    }
}
