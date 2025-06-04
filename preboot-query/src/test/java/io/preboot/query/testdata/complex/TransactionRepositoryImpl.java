package io.preboot.query.testdata.complex;

import io.preboot.query.FilterableFragmentContext;
import io.preboot.query.FilterableFragmentImpl;
import org.springframework.stereotype.Repository;

@Repository
class TransactionRepositoryImpl extends FilterableFragmentImpl<Transaction, Long> {
    public TransactionRepositoryImpl(FilterableFragmentContext context) {
        super(context, Transaction.class);
    }
}
