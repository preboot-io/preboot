package io.preboot.query.testdata;

import io.preboot.query.FilterableFragmentContext;
import io.preboot.query.FilterableFragmentImpl;
import org.springframework.stereotype.Repository;

@Repository
class ProductRepositoryImpl extends FilterableFragmentImpl<Product, Long> {
    public ProductRepositoryImpl(FilterableFragmentContext context) {
        super(context, Product.class);
    }
}
