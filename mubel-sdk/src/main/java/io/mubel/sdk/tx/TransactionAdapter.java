package io.mubel.sdk.tx;

/**
 * A transaction adapter.
 *
 * This interface is used to execute transactions.
 */
public interface TransactionAdapter {

    void execute(Runnable runnable);

    static TransactionAdapter noOpTransactionAdapter() {
        return Runnable::run;
    }

}
