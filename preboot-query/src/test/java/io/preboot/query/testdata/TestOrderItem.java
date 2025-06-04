package io.preboot.query.testdata;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("order_items")
public class TestOrderItem {
    @Id
    private Long id;

    private String productCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
