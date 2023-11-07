package io.mubel.spring;

import io.mubel.sdk.eventstore.EventStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@Testcontainers
@SpringBootTest(classes = TestApplication.class)
@TestPropertySource("classpath:mubel-test.properties")
public class MubelAutoConfigurationTest {

    @Container
    @ServiceConnection(name = "mubel")
    static GenericContainer<?> mubelContainer = new GenericContainer<>("mubel:latest")
            .withExposedPorts(9898);

    @Autowired
    MubelProperties properties;

    @Autowired
    EventStore eventStore;

    @Test
    void baseCase() {
        assertThat(properties.getUri())
                .isEqualTo(URI.create("mubel://localhost:9898"));

        assertThat(eventStore.get(UUID.randomUUID().toString())).isEmpty();
    }
}