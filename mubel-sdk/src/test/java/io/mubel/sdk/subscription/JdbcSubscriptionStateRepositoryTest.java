package io.mubel.sdk.subscription;

import io.mubel.sdk.exceptions.MubelException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static io.mubel.sdk.testutils.DataSourceUtil.createPgDataSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class JdbcSubscriptionStateRepositoryTest {


    @Container
    static final JdbcDatabaseContainer<?> dbContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
            .withDatabaseName("mubel-sdk-test")
            .withUsername("mubel")
            .withPassword("mubel")
            .withInitScript("subscription_state_pg_init.sql");

    static JdbcSubscriptionStateRepository repository;

    @BeforeAll
    static void setup() {
        repository = new JdbcSubscriptionStateRepository(createPgDataSource(dbContainer));
    }

    @Test
    void stateDoesNotExists() {
        assertThat(repository.find("does_not_exists"))
                .isEmpty();
    }

    @Test
    void putAndGet() {
        final var consumerGroup = "test-1";
        final var state = SubscriptionState.initialState(consumerGroup)
                .withSequenceNumber(1);
        repository.put(state);
        assertThat(repository.find(consumerGroup))
                .contains(state);
        final var nextState = state.withSequenceNumber(9000);
        repository.put(nextState);
        assertThat(repository.find(consumerGroup))
                .contains(nextState);
    }

    @Test
    public void putWithStaleVersion() {
        final var consumerGroup = "test-2";
        final var state = SubscriptionState.initialState(consumerGroup)
                .withSequenceNumber(1);
        repository.put(state);
        final var nextState = state.withSequenceNumber(9000);
        repository.put(nextState);
        assertThatThrownBy(() -> repository.put(state.withSequenceNumber(9001)))
                .as("Put a state with same version as existing state should throw")
                .isInstanceOf(MubelException.class)
                .hasMessage("Optimistic lock error: Stale state: SubscriptionState[consumerGroup=test-2, sequenceNumber=9001, version=1]");
    }

    @Test
    public void putDuplicateInitialState() {
        final var consumerGroup = "test-3";
        final var state = SubscriptionState.initialState(consumerGroup)
                .withSequenceNumber(1);
        repository.put(state);
        assertThatThrownBy(() -> repository.put(state))
                .as("Put a state with same version as existing state should throw")
                .isInstanceOf(MubelException.class)
                .hasMessage("Optimistic lock error: Stale state: SubscriptionState[consumerGroup=test-3, sequenceNumber=1, version=0]");
    }

}