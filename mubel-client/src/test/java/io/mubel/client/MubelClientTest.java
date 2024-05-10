package io.mubel.client;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled("server is not ready yet")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
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
    void Client_can_connect_and_get_server_info() {
        assertNotNull(client.getServerInfo());
    }
}