package io.mubel.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * @param address            The mubel address to connect to.
 * @param eventStoreId       The event store id to use.
 * @param storageBackendName The storage backend name to connect to
 * @param idGenerator        The id generation strategy to use. Default ORDERED.
 */
@ConfigurationProperties(prefix = "mubel")
public record MubelProperties(
        String address,
        String eventStoreId,
        String storageBackendName,
        IdGenerationStrategy idGenerator
) {

    public MubelProperties {
        idGenerator = Objects.requireNonNullElse(idGenerator, IdGenerationStrategy.ORDERED);
    }

    public enum IdGenerationStrategy {
        /**
         * Generate ids that are sortable by time. This strategy is a good choice for relational databases such as PostgreSql.
         */
        ORDERED,
        /**
         * Generate ids that are random. This strategy is a good choice for NoSQL databases.
         */
        RANDOM
    }
}
