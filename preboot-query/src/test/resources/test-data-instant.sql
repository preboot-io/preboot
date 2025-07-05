-- Test data for Instant handling tests
CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    event_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_value DECIMAL(19,2),
    event_timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

INSERT INTO events (event_name, event_type, event_value, event_timestamp, created_at, updated_at) VALUES
    ('User Registration', 'USER_EVENT', 0.00, '2024-01-01T10:00:00Z', '2024-01-01T10:00:00Z', '2024-01-01T10:00:00Z'),
    ('Payment Processed', 'PAYMENT_EVENT', 99.99, '2024-01-02T11:30:00Z', '2024-01-02T11:30:00Z', '2024-01-02T11:30:00Z'),
    ('Order Created', 'ORDER_EVENT', 149.50, '2024-01-03T14:45:00Z', '2024-01-03T14:45:00Z', '2024-01-03T14:45:00Z'),
    ('User Login', 'USER_EVENT', 0.00, '2024-01-04T09:15:00Z', '2024-01-04T09:15:00Z', '2024-01-04T09:15:00Z'),
    ('Payment Failed', 'PAYMENT_EVENT', 75.25, '2024-01-05T16:20:00Z', '2024-01-05T16:20:00Z', '2024-01-05T16:20:00Z'),
    ('Order Shipped', 'ORDER_EVENT', 200.00, '2024-01-06T08:30:00Z', '2024-01-06T08:30:00Z', null),
    ('User Logout', 'USER_EVENT', 0.00, '2024-01-07T18:45:00Z', '2024-01-07T18:45:00Z', '2024-01-07T18:45:00Z');