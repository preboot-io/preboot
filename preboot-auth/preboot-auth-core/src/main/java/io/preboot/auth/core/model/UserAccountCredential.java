package io.preboot.auth.core.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_account_credentials")
@Data
@Accessors(chain = true)
public class UserAccountCredential {
    private String type;
    private String attribute;
}
