package io.mubel.spring;

import io.mubel.sdk.eventstore.EventStore;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@Testcontainers
@SpringBootTest(classes = TestApplication.class)
@TestPropertySource("classpath:mubel-test.properties")
public class MubelAutoConfigurationTest {

    static final Logger LOG = LoggerFactory.getLogger(MubelAutoConfigurationTest.class);

    @Container
    @ServiceConnection(name = "mubel")
    static GenericContainer<?> mubelContainer = new GenericContainer<>("mubel-server:latest")
            .withExposedPorts(9090)
            .withLogConsumer(new Slf4jLogConsumer(LOG));

    @Autowired
    MubelProperties properties;

    @Autowired
    EventStore eventStore;

    @Test
    void baseCase() {
        assertThat(properties.address())
                .isEqualTo("localhost:9898");

        assertThat(eventStore.get(UUID.randomUUID().toString())).isEmpty();
    }
}