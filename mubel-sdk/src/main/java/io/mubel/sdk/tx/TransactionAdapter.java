package io.mubel.sdk.tx;

public interface TransactionAdapter {

    void execute(Runnable runnable);

    static TransactionAdapter noOpTransactionAdapter() {
        return Runnable::run;
    }

}
