package io.mubel.sdk.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executor;

public class ScheduledEventsManager {

    private final static Logger LOG = LoggerFactory.getLogger(ScheduledEventsManager.class);
    private final Executor executor;
    private final List<ScheduledEventsConfig<?>> configs;
    private final ScheduledEventsWorker worker;

    public ScheduledEventsManager(Builder b) {
        this.executor = b.executor;
        this.configs = b.configs;
        this.worker = b.worker;
    }

    public void start() {
        if (configs.isEmpty()) {
            LOG.warn("No subscriptions configured");
        }
        configs.forEach(this::startSubscription);
    }

    private void startSubscription(ScheduledEventsConfig<?> subscriptionConfig) {
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Executor executor;
        private List<ScheduledEventsConfig<?>> configs = List.of();
        private ScheduledEventsWorker worker;

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder configs(List<ScheduledEventsConfig<?>> configs) {
            this.configs = configs;
            return this;
        }

        public Builder worker(ScheduledEventsWorker worker) {
            this.worker = worker;
            return this;
        }

        public ScheduledEventsManager build() {
            return new ScheduledEventsManager(this);
        }
    }
}
