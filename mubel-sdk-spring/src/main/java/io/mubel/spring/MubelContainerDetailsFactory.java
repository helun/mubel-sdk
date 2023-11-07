package io.mubel.spring;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.testcontainers.containers.GenericContainer;

import java.net.URI;

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
        public URI getUri() {
            final var container = this.getContainer();
            final int port = container.getFirstMappedPort();
            final var host = container.getHost();
            return URI.create(String.format("mubel://%s:%d", host, port));
        }

        public MubelContainerConnectionDetails(ContainerConnectionSource<GenericContainer<?>> source) {
            super(source);
        }

    }
}
