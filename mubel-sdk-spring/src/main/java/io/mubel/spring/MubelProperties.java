package io.mubel.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "mubel")
public class MubelProperties {
    /**
     * The mubel uri to connect to.
     */
    private URI uri;
    /**
     * The event store id to use.
     */
    private String eventStoreId;
    /**
     * The id generation strategy to use. Default TIMEBASED.
     */
    private IdGenerationStrategy idGenerator = IdGenerationStrategy.TIMEBASED;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
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
        TIMEBASED,
        RANDOM
    }
}
