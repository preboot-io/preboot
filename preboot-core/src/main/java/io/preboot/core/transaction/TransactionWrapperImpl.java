package io.preboot.core.transaction;

import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class TransactionWrapperImpl implements TransactionWrapper {
    @Transactional
    @Override
    public <T> T doInTransaction(Supplier<T> action) {
        return action.get();
    }

    @Transactional
    @Override
    public void doInTransaction(Runnable action) {
        action.run();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public <T> T doAlwaysInNewTransaction(Supplier<T> action) {
        return action.get();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void doAlwaysInNewTransaction(Runnable action) {
        action.run();
    }
}
