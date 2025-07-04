-- Test data for enum handling tests
CREATE TABLE IF NOT EXISTS orders_with_enum (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    tags TEXT[]
);

INSERT INTO orders_with_enum (order_number, amount, status, created_at, tags) VALUES
    ('ENUM001', 100.00, 'COMPLETED', '2024-01-01 10:00:00', ARRAY['express', 'priority']),
    ('ENUM002', 200.00, 'PENDING', '2024-01-02 11:00:00', ARRAY['standard', 'discount']),
    ('ENUM003', 300.00, 'COMPLETED', '2024-01-03 12:00:00', ARRAY['priority', 'gift']),
    ('ENUM004', 400.00, 'CANCELLED', '2024-01-04 13:00:00', ARRAY['refund', 'complaint']),
    ('ENUM005', 500.00, 'PENDING', '2024-01-05 14:00:00', ARRAY['standard', 'discount', 'promotion']),
    ('ENUM006', 600.00, 'CANCELLED', '2024-01-06 15:00:00', ARRAY['refund']),
    ('ENUM007', 700.00, 'COMPLETED', '2024-01-07 16:00:00', ARRAY['priority']);