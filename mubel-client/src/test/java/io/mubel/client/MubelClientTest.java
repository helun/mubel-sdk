package io.mubel.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
class MubelClientTest {

    @Container
    static GenericContainer<?> mubelContainer = new GenericContainer<>("mubel:latest")
            .withExposedPorts(9898);

    MubelClient client;

    @BeforeEach
    void setup() {
        client = new MubelClient(MubelClientConfig.newBuilder()
                .address("localhost:" + mubelContainer.getMappedPort(9898))
                .build());
    }

    @Test
    void basicConnect() {
        assertNotNull(client.getServerInfo());
    }
}