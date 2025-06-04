package io.preboot.query.testdata;

import io.preboot.query.FilterableFragmentContext;
import io.preboot.query.FilterableFragmentImpl;
import org.springframework.stereotype.Repository;

@Repository
class TestOrderRepositoryImpl extends FilterableFragmentImpl<TestOrder, Long> {
    public TestOrderRepositoryImpl(FilterableFragmentContext context) {
        super(context, TestOrder.class);
    }
}
