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
import io.mubel.sdk.internal.Constants;
import io.mubel.sdk.scheduled.ExpiredDeadlineConsumer;
import io.mubel.sdk.scheduled.ExpiredDeadlineHandler;
import io.mubel.sdk.subscription.*;
import io.mubel.sdk.tx.TransactionAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.system.JavaVersion;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                .address(connectionDetails.getAddress())
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
                .eventStoreId(properties.eventStoreId())
                .client(mubelClient)
                .build();
    }

    @Bean(initMethod = "provision")
    @ConditionalOnMissingBean
    @ConditionalOnBean(MubelClient.class)
    public EventStoreProvisioner eventStoreProvisioner(
            MubelProperties properties,
            MubelClient eventsClient) {
        return EventStoreProvisioner.builder()
                .eventStore(properties.eventStoreId(), EventStoreProvisioner.DataFormat.JSON, properties.storageBackendName())
                .client(eventsClient)
                .build();
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    public IdGenerator idGenerator(MubelProperties properties) {
        if (properties.idGenerator() == MubelProperties.IdGenerationStrategy.ORDERED) {
            System.setProperty(Constants.ID_GENERATOR_TYPE_KEY, "ordered");
            return IdGenerator.timebasedGenerator();
        } else {
            System.setProperty(Constants.ID_GENERATOR_TYPE_KEY, "random");
            return IdGenerator.randomGenerator();
        }
    }

    @Bean
    @Lazy
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
    @Lazy
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
    @Lazy
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
    @Lazy
    @ConditionalOnMissingBean
    public TransactionAdapter noOpTransactionAdapter() {
        return TransactionAdapter.noOpTransactionAdapter();
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataSource.class)
    public SubscriptionStateRepository subscriptionStateRepository(DataSource dataSource) {
        return new JdbcSubscriptionStateRepository(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(SubscriptionStateRepository.class)
    public SubscriptionWorker subscriptionWorker(
            MubelClient client,
            TransactionAdapter transactionAdapter,
            EventDataMapper eventDataMapper,
            SubscriptionStateRepository subscriptionStateRepository
    ) {
        return SubscriptionWorker.builder()
                .client(client)
                .stateRepository(subscriptionStateRepository)
                .transactionAdapter(transactionAdapter)
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

    @Bean
    @ConditionalOnMissingBean
    public ExpiredDeadlineHandler expiredDeadlineHandler(
            MubelProperties properties,
            List<ExpiredDeadlineConsumer> consumers,
            MubelClient client,
            EventDataMapper eventDataMapper
    ) {
        return ExpiredDeadlineHandler.builder()
                .esid(properties.eventStoreId())
                .consumers(consumers)
                .client(client)
                .eventDataMapper(eventDataMapper)
                .build();
    }

    @Bean
    @ConditionalOnBean(ExpiredDeadlineHandler.class)
    public ApplicationListener<ContextRefreshedEvent> expiredDeadlineHandlerStarter(
            ExpiredDeadlineHandler handler
    ) {
        return event -> handler.start();
    }

    @ConditionalOnMissingBean
    @ConditionalOnJava(range = ConditionalOnJava.Range.EQUAL_OR_NEWER, value = JavaVersion.TWENTY_ONE)
    public Executor virtialThreadsTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @ConditionalOnMissingBean
    @ConditionalOnJava(range = ConditionalOnJava.Range.OLDER_THAN, value = JavaVersion.TWENTY_ONE)
    public Executor taskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("mubel-");
        executor.setQueueCapacity(0);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }

    @Bean
    @ConditionalOnBean(name = "mubelTaskExecutor")
    public ApplicationListener<ContextClosedEvent> stopWorkers(@Qualifier("mubelTaskExecutor") Executor executor) {
        return event -> {
            if (executor instanceof ThreadPoolTaskExecutor tpe) {
                tpe.shutdown();
            } else if (executor instanceof ExecutorService es) {
                es.shutdownNow();
            }
        };
    }

    static class PropertiesMubelConnectionDetails implements MubelConnectionDetails {

        private final MubelProperties properties;

        PropertiesMubelConnectionDetails(MubelProperties properties) {
            this.properties = properties;
        }

        @Override
        public String getAddress() {
            var address = this.properties.address();
            return (address != null) ? address : MubelConnectionDetails.super.getAddress();
        }
    }

}
