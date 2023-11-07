package io.mubel.sdk.subscription;

import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executor;

public class SubscriptionManager {

    private final static Logger LOG = LoggerFactory.getLogger(SubscriptionManager.class);

    private final Executor executor;
    private final List<SubscriptionConfig<?>> configs;

    private final SubscriptionWorker worker;

    private SubscriptionManager(SubscriptionManagerBuilder b) {
        this.executor = b.executor;
        this.configs = b.configs;
        this.worker = b.worker;
    }

    public static SubscriptionManagerBuilder builder() {
        return new SubscriptionManagerBuilder();
    }

    public void start() {
        if (configs.isEmpty()) {
            LOG.warn("No subscriptions configured");
        }
        configs.forEach(this::startSubscription);
    }

    private void startSubscription(SubscriptionConfig<?> subscriptionConfig) {
        LOG.info("Starting subscription: {}", subscriptionConfig);
        executor.execute(() -> {
            try {
                worker.start(subscriptionConfig);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }

    public static class SubscriptionManagerBuilder {
        private Executor executor;
        private List<SubscriptionConfig<?>> configs = List.of();
        private SubscriptionWorker worker;

        public SubscriptionManagerBuilder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public SubscriptionManagerBuilder configs(List<SubscriptionConfig<?>> configs) {
            this.configs = configs;
            return this;
        }

        public SubscriptionManagerBuilder worker(SubscriptionWorker worker) {
            this.worker = worker;
            return this;
        }

        public SubscriptionManager build() {
            Utils.requireNonNull(executor, () -> new MubelConfigurationException("Executor may not be null"));
            Utils.requireNonNull(worker, () -> new MubelConfigurationException("Worker may not be null"));
            return new SubscriptionManager(this);
        }
    }

}
