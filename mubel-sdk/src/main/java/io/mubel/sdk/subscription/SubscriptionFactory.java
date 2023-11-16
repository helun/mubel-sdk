package io.mubel.sdk.subscription;

import io.mubel.api.grpc.EventData;
import io.mubel.api.grpc.SubscribeRequest;
import io.mubel.client.ExceptionHandler;
import io.mubel.client.MubelClient;
import io.mubel.sdk.exceptions.MubelConfigurationException;
import io.mubel.sdk.internal.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class SubscriptionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionFactory.class);

    private final MubelClient client;
    private final Executor executor;

    public static Builder builder() {
        return new Builder();
    }

    private SubscriptionFactory(Builder b) {
        this.client = b.client;
        this.executor = b.executor;
    }

    public Subscription create(SubscriptionConfig<?> params, long fromSequenceNo) {
        final var buffer = new ArrayBlockingQueue<EventData>(calculateBufferSize(params));
        start(buffer, params, fromSequenceNo);
        return new Subscription(buffer);
    }

    private static int calculateBufferSize(SubscriptionConfig<?> params) {
        return params.batchSize() * 2;
    }

    private void start(BlockingQueue<EventData> buffer, SubscriptionConfig<?> params, long fromSequenceNo) {
        final var request = SubscribeRequest.newBuilder()
                .setEsid(params.eventStoreId())
                .setFromSequenceNo(fromSequenceNo)
                .build();

        executor.execute(() -> {
            LOG.info("Subscription (esid: {}, from version: {}) stream starting", params.eventStoreId(), fromSequenceNo);
            try {
                final var stream = client.subscribe(request);
                while (stream.hasNext()) {
                    final var e = stream.next();
                    offer(buffer, e);
                }
                LOG.info("Subscription (esid: {}, from version: {}) stream stopped", params.eventStoreId(), fromSequenceNo);
            } catch (InterruptedException e) {
                LOG.error("Subscription (esid: {}, from version: {}) stream was interrupted", params.eventStoreId(), fromSequenceNo);
                Thread.currentThread().interrupt();
            } catch (Throwable err) {
                final var mapped = ExceptionHandler.handleFailure(err);
                LOG.error("Subscription (esid: {}, from version: {}) stream stopped", params.eventStoreId(), fromSequenceNo, mapped);
            }
        });
    }

    private static void offer(BlockingQueue<EventData> buffer, EventData e) throws InterruptedException {
        while (!buffer.offer(e, 1, TimeUnit.MINUTES)) {
            LOG.warn("Buffer is full, retrying: sequence No {}", e.getSequenceNo());
        }
    }

    public static class Builder {
        private MubelClient client;

        private Executor executor;

        /**
         * Mubel client used to subscribe to the event stream.
         *
         * @param client
         * @return
         */
        public Builder client(MubelClient client) {
            this.client = client;
            return this;
        }

        /**
         * Executor used to run the subscription stream.
         *
         * @param executor
         * @return this
         */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public SubscriptionFactory build() {
            Utils.requireNonNull(
                    client,
                    () -> new MubelConfigurationException("client must not be null")
            );
            Utils.requireNonNull(
                    executor,
                    () -> new MubelConfigurationException("executor must not be null")
            );
            return new SubscriptionFactory(this);
        }
    }
}
