package io.preboot.core.transaction;

import java.util.function.Supplier;

public interface TransactionWrapper {
    <T> T doInTransaction(Supplier<T> action);

    void doInTransaction(Runnable action);

    <T> T doAlwaysInNewTransaction(Supplier<T> action);

    void doAlwaysInNewTransaction(Runnable action);
}
