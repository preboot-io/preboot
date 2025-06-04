package io.preboot.query.testdata;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("orders")
public class TestOrder {
    @Id
    private Long id;

    private String orderNumber;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;

    private List<String> tags;

    @MappedCollection(idColumn = "order_id")
    private Set<TestOrderItem> orderItems = new HashSet<>();
}
