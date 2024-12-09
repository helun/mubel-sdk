package io.mubel.spring;

import io.mubel.sdk.Constrains;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
        @NotNull @Pattern(regexp = Constrains.ADDRESS_REGEXP, message = "Invalid address, must be a network address, such as localhost:9090 or 192.168.0.1:9090")
        String address,
        @NotNull @Pattern(regexp = Constrains.ESID_REGEXP, message = "Invalid event store id, must be an alpha numeric string and may be delimited by '.' (period) or ':' colon")
        String eventStoreId,
        @NotNull @Pattern(regexp = Constrains.SAFE_STRING_REGEXP)
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
