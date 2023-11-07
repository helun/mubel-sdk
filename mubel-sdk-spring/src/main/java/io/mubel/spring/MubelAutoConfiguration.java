package io.mubel.spring;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mubel.client.MubelClient;
import io.mubel.client.MubelClientConfig;
import io.mubel.sdk.EventDataMapper;
import io.mubel.sdk.EventNamingStrategy;
import io.mubel.sdk.EventTypeRegistry;
import io.mubel.sdk.IdGenerator;
import io.mubel.sdk.codec.JacksonJsonEventDataCodec;
import io.mubel.sdk.eventstore.DefaultEventStore;
import io.mubel.sdk.eventstore.EventStore;
import io.mubel.sdk.eventstore.EventStoreProvisioner;
import io.mubel.sdk.subscription.*;
import io.mubel.sdk.tx.TransactionAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executor;

@AutoConfiguration(after = {
        DataSourceAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        TransactionAutoConfiguration.class
})
@EnableConfigurationProperties(MubelProperties.class)
public class MubelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MubelConnectionDetails.class)
    public MubelConnectionDetails mubelConnectionDetails(MubelProperties properties) {
        return new PropertiesMubelConnectionDetails(properties);
    }

    @Bean
    @ConditionalOnBean(MubelConnectionDetails.class)
    public MubelClient mubelClient(MubelConnectionDetails connectionDetails) {
        final var config = MubelClientConfig.newBuilder()
                .host(connectionDetails.getUri().getHost())
                .port(connectionDetails.getUri().getPort())
                .build();

        return new MubelClient(config);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "mubel.event-store-id")
    @ConditionalOnBean(MubelClient.class)
    public EventStore eventStore(
            MubelProperties properties,
            MubelClient mubelClient
    ) {
        return DefaultEventStore.builder()
                .eventStoreId(properties.getEventStoreId())
                .client(mubelClient)
                .build();
    }

    @Bean(initMethod = "provision")
    @ConditionalOnMissingBean
    public EventStoreProvisioner eventStoreProvisioner(
            MubelProperties properties,
            MubelClient eventsClient) {
        return EventStoreProvisioner.builder()
                .eventStore(properties.getEventStoreId(), EventStoreProvisioner.DataFormat.JSON)
                .client(eventsClient)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdGenerator idGenerator(MubelProperties properties) {
        return properties.getIdGenerator() == MubelProperties.IdGenerationStrategy.TIMEBASED ?
                IdGenerator.timebasedGenerator() : IdGenerator.randomGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventTypeRegistry eventTypeRegistry() {
        return EventTypeRegistry.builder()
                .withNamingStrategy(EventNamingStrategy.byClass())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    @ConditionalOnClass(ObjectMapper.class)
    public ObjectMapper mubelJacksonObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Bean
    @ConditionalOnBean(ObjectMapper.class)
    public EventDataMapper jacksonEventDataMapper(
            ObjectMapper jsonMapper,
            IdGenerator idGenerator,
            EventTypeRegistry eventTypeRegistry
    ) {
        return new EventDataMapper(
                new JacksonJsonEventDataCodec(jsonMapper),
                eventTypeRegistry,
                idGenerator
        );
    }

    @Bean
    @ConditionalOnBean({MubelClient.class, Executor.class})
    public SubscriptionFactory subscriptionFactory(
            MubelClient client,
            Executor executor
    ) {
        return SubscriptionFactory.builder()
                .client(client)
                .executor(executor)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(TransactionTemplate.class)
    public TransactionAdapter springTxTransactionAdapter(TransactionTemplate txTemplate) {
        return action -> txTemplate.execute(status -> {
            try {
                action.run();
                status.flush();
            } catch (Throwable err) {
                status.setRollbackOnly();
                throw err;
            }
            return null;
        });
    }

    @Bean
    @ConditionalOnMissingBean
    public TransactionAdapter noOpTransactionAdapter() {
        return TransactionAdapter.noOpTransactionAdapter();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataSource.class)
    public SubscriptionStateRepository subscriptionStateRepository(DataSource dataSource) {
        return new JdbcSubscriptionStateRepository(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(SubscriptionStateRepository.class)
    public SubscriptionWorker subscriptionWorker(
            TransactionAdapter transactionAdapter,
            EventDataMapper eventDataMapper,
            SubscriptionStateRepository subscriptionStateRepository,
            SubscriptionFactory subscriptionFactory
    ) {
        return SubscriptionWorker.builder()
                .stateRepository(subscriptionStateRepository)
                .transactionAdapter(transactionAdapter)
                .subscriptionFactory(subscriptionFactory)
                .eventDataMapper(eventDataMapper)
                .build();
    }

    @Bean
    @ConditionalOnBean({SubscriptionWorker.class, Executor.class})
    public SubscriptionManager subscriptionManager(
            List<SubscriptionConfig<?>> subscriptionConfigs,
            SubscriptionWorker subscriptionWorker,
            Executor executor
    ) {
        return SubscriptionManager.builder()
                .configs(subscriptionConfigs)
                .worker(subscriptionWorker)
                .executor(executor)
                .build();
    }

    @Bean
    @ConditionalOnBean(SubscriptionManager.class)
    public ApplicationListener<ContextRefreshedEvent> subscriptionManagerStarter(
            SubscriptionManager subscriptionManager
    ) {
        return event -> subscriptionManager.start();
    }

    static class PropertiesMubelConnectionDetails implements MubelConnectionDetails {

        private final MubelProperties properties;

        PropertiesMubelConnectionDetails(MubelProperties properties) {
            this.properties = properties;
        }

        @Override
        public URI getUri() {
            URI uri = this.properties.getUri();
            return (uri != null) ? uri : MubelConnectionDetails.super.getUri();
        }
    }

}