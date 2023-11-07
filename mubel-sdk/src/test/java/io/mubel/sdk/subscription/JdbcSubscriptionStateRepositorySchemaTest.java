package io.mubel.sdk.subscription;

import io.mubel.sdk.exceptions.MubelConfigurationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.stream.Stream;

import static io.mubel.sdk.testutils.DataSourceUtil.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
public class JdbcSubscriptionStateRepositorySchemaTest {

    @Container
    static final JdbcDatabaseContainer<?> pgContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
            .withDatabaseName("mubel-sdk-test")
            .withUsername("mubel")
            .withPassword("mubel");

    @Container
    static final JdbcDatabaseContainer<?> mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"))
            .withDatabaseName("mubel-sdk-test")
            .withUsername("mubel")
            .withPassword("mubel");

    @Container
    static final JdbcDatabaseContainer<?> msSqlServer = new MSSQLServerContainer<>(
            DockerImageName.parse("mcr.microsoft.com/azure-sql-edge")
                    .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"))
            .acceptLicense()
            .withPassword("irH7XYhaxoTcHm.ft-DaqsCH");

    @ParameterizedTest
    @MethodSource("dataSources")
    void missingSchemaPostgres(DataSource ds) {
        final var repository = new JdbcSubscriptionStateRepository(ds);
        assertThatThrownBy(() -> repository.find("does_not_exists"))
                .isInstanceOf(MubelConfigurationException.class)
                .hasMessageContaining("subscription_state table does not exist. Here is the DDL:");
    }

    static Stream<DataSource> dataSources() {
        return Stream.of(
                createPgDataSource(pgContainer),
                createMysqlDataSource(mysqlContainer),
                createMsSqlDataSource(msSqlServer)
        );
    }
}
