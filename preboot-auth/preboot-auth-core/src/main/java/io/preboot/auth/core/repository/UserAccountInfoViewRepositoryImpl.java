package io.preboot.auth.core.repository;

import io.preboot.auth.core.model.UserAccountInfoView;
import io.preboot.query.FilterableFragmentContext;
import io.preboot.query.FilterableFragmentImpl;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountInfoViewRepositoryImpl extends FilterableFragmentImpl<UserAccountInfoView, Long> {

    protected UserAccountInfoViewRepositoryImpl(final FilterableFragmentContext context) {
        super(context, UserAccountInfoView.class);
    }
}
