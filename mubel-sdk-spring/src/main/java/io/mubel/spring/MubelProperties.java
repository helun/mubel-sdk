package io.mubel.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mubel")
public class MubelProperties {
    /**
     * The mubel address to connect to.
     */
    private String address;
    /**
     * The event store id to use.
     */
    private String eventStoreId;
    /**
     * The id generation strategy to use. Default ORDERED.
     */
    private IdGenerationStrategy idGenerator = IdGenerationStrategy.ORDERED;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEventStoreId() {
        return eventStoreId;
    }

    public void setEventStoreId(String eventStoreId) {
        this.eventStoreId = eventStoreId;
    }

    public IdGenerationStrategy getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(IdGenerationStrategy idGenerator) {
        this.idGenerator = idGenerator;
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
