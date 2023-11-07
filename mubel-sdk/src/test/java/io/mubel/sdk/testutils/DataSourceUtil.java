package io.mubel.sdk.testutils;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;

public class DataSourceUtil {

    public static DataSource createPgDataSource(JdbcDatabaseContainer<?> dbContainer) {
        final var ds = new PGSimpleDataSource();
        ds.setPortNumbers(new int[]{dbContainer.getFirstMappedPort()});
        ds.setDatabaseName(dbContainer.getDatabaseName());
        ds.setUser(dbContainer.getUsername());
        ds.setPassword(dbContainer.getPassword());
        return ds;
    }

    public static DataSource createMysqlDataSource(JdbcDatabaseContainer<?> dbContainer) {
        final var ds = new MysqlDataSource();
        ds.setPort(dbContainer.getFirstMappedPort());
        ds.setDatabaseName(dbContainer.getDatabaseName());
        ds.setUser(dbContainer.getUsername());
        ds.setPassword(dbContainer.getPassword());
        return ds;
    }

    public static DataSource createMsSqlDataSource(JdbcDatabaseContainer<?> dbContainer) {
        final var ds = new SQLServerDataSource();
        ds.setPortNumber(dbContainer.getFirstMappedPort());
        ds.setUser(dbContainer.getUsername());
        ds.setEncrypt("false");
        ds.setPassword(dbContainer.getPassword());
        return ds;
    }
}
