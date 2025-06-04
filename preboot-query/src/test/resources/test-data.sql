CREATE TABLE IF NOT EXISTS orders (
                                    id BIGSERIAL PRIMARY KEY,
                                    order_number VARCHAR(50) NOT NULL,
                                    amount DECIMAL(19,2) NOT NULL,
                                    status VARCHAR(20) NOT NULL,
                                    created_at TIMESTAMP NOT NULL,
                                    tags TEXT[]
);

CREATE TABLE IF NOT EXISTS order_items (
                                           id BIGSERIAL PRIMARY KEY,
                                           order_id BIGINT NOT NULL,
                                           product_code VARCHAR(50) NOT NULL,
                                           quantity INTEGER NOT NULL,
                                           unit_price DECIMAL(19,2) NOT NULL,
                                           total_price DECIMAL(19,2) NOT NULL,
                                           FOREIGN KEY (order_id) REFERENCES orders(id)
);

INSERT INTO orders (order_number, amount, status, created_at, tags) VALUES
                                                                        ('ORD001', 100.00, 'COMPLETED', '2024-01-01 10:00:00', ARRAY['express', 'priority']),
                                                                        ('ORD002', 200.00, 'PENDING', '2024-01-02 11:00:00', ARRAY['standard', 'discount']),
                                                                        ('ORD003', 300.00, 'COMPLETED', '2024-01-03 12:00:00', ARRAY['priority', 'gift']),
                                                                        ('ORD004', 400.00, 'CANCELLED', '2024-01-04 13:00:00', ARRAY['refund', 'complaint']),
                                                                        ('ORD005', 500.00, 'PENDING', '2024-01-05 14:00:00', ARRAY['standard', 'discount', 'promotion']);

INSERT INTO order_items (order_id, product_code, quantity, unit_price, total_price) VALUES
                                                                                        (1, 'PROD-A', 2, 50.00, 100.00),
                                                                                        (1, 'PROD-B', 1, 50.00, 50.00),
                                                                                        (2, 'PROD-C', 2, 100.00, 200.00),
                                                                                        (3, 'PROD-A', 3, 100.00, 300.00),
                                                                                        (4, 'PROD-B', 4, 100.00, 400.00),
                                                                                        (5, 'PROD-C', 5, 100.00, 500.00);
