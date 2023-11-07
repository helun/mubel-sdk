package io.mubel.sdk.subscription;

import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.exceptions.MubelException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * This is an implementation of {@link SubscriptionStateRepository} that uses a JDBC connection to store the subscription state.
 * <p>
 * DDL for the table:
 * <pre>
 * CREATE TABLE subscription_state (
 *     consumer_group VARCHAR(255) PRIMARY KEY,
 *     sequence_no BIGINT NOT NULL,
 *     version INT NOT NULL
 * );
 * </pre>
 */
public class JdbcSubscriptionStateRepository implements SubscriptionStateRepository {

    private static final String FIND_STATE_SQL = """
            SELECT consumer_group, sequence_no, version
            FROM subscription_state
            WHERE consumer_group = ?
            """;
    private static final String INSERT_STATE_SQL = """
            INSERT INTO subscription_state (consumer_group, sequence_no, version) VALUES (?, ?, ?)
            """;
    private static final String UPDATE_STATE_SQL = """
            UPDATE subscription_state SET sequence_no = ?, version = ?
            WHERE consumer_group = ?
            AND version = ?
            """;

    private static final Set<String> TABLE_DOES_NOT_EXIST_SQL_STATES = Set.of(
            "42P01", // Postgres
            "42S02", // MySQL
            "S0002" // MS SQL Server
    );

    private final DataSource dataSource;

    public JdbcSubscriptionStateRepository(DataSource dataSource) {
        this.dataSource = requireNonNull(dataSource, "dataSource may not be null");
    }

    @Override
    public Optional<SubscriptionState> find(String consumerGroup) {
        try (final var conn = dataSource.getConnection()) {
            try (final var stmt = conn.prepareStatement(FIND_STATE_SQL)) {
                stmt.setString(1, consumerGroup);
                try (final var rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new SubscriptionState(
                                rs.getString(1),
                                rs.getLong(2),
                                rs.getInt(3)
                        ));
                    }
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw handleSqlException(e);
        }
    }

    private static RuntimeException handleSqlException(SQLException e) {
        final var sqlState = e.getSQLState();
        if (TABLE_DOES_NOT_EXIST_SQL_STATES.contains(sqlState)) {
            return new MubelConfigurationException("""
                    subscription_state table does not exist. Here is the DDL:
                    CREATE TABLE subscription_state (
                        consumer_group VARCHAR(255) PRIMARY KEY,
                        sequence_no BIGINT NOT NULL,
                        version INT NOT NULL
                    );
                    """);
        }
        return new RuntimeException(e);
    }

    @Override
    public void put(SubscriptionState state) {
        try (final var conn = dataSource.getConnection()) {
            if (state.version() > SubscriptionState.FIRST_VERSION) {
                updateState(state, conn);
            } else {
                doInsertState(state, conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateState(SubscriptionState state, Connection conn) {
        try (final var stmt = conn.prepareStatement(UPDATE_STATE_SQL)) {
            stmt.setLong(1, state.sequenceNumber());
            stmt.setInt(2, state.version());
            stmt.setString(3, state.consumerGroup());
            stmt.setInt(4, state.version() - 1);
            var updatedRows = stmt.executeUpdate();
            if (updatedRows != 1) {
                throw createOptimisticLockException(state);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void doInsertState(SubscriptionState state, Connection conn) throws SQLException {
        if (find(state.consumerGroup()).isPresent()) {
            throw createOptimisticLockException(state);
        }
        insertState(state, conn);
    }

    private void insertState(SubscriptionState state, Connection conn) throws SQLException {
        try (final var stmt = conn.prepareStatement(INSERT_STATE_SQL)) {
            stmt.setString(1, state.consumerGroup());
            stmt.setLong(2, state.sequenceNumber());
            stmt.setInt(3, state.version());
            stmt.executeUpdate();
        }
    }

    private static MubelException createOptimisticLockException(SubscriptionState state) {
        return new MubelException("Optimistic lock error: Stale state: %s"
                .formatted(state)
        );
    }
}
