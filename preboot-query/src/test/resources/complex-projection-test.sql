-- Create tables
CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            uuid UUID NOT NULL UNIQUE,
                            name VARCHAR(255) NOT NULL,
                            color VARCHAR(7) NOT NULL
);

CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              uuid UUID NOT NULL UNIQUE,
                              name VARCHAR(255) NOT NULL,
                              amount DECIMAL(19,2) NOT NULL,
                              type VARCHAR(20) NOT NULL,
                              transaction_date DATE NOT NULL
);

CREATE TABLE transaction_categories (
                                        id BIGSERIAL PRIMARY KEY,
                                        transaction_id BIGINT NOT NULL,
                                        category_uuid UUID NOT NULL,
                                        FOREIGN KEY (transaction_id) REFERENCES transactions(id),
                                        FOREIGN KEY (category_uuid) REFERENCES categories(uuid)
);

-- Insert test data
INSERT INTO categories (uuid, name, color) VALUES
                                               ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Food', '#FF0000'),
                                               ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'Household', '#00FF00'),
                                               ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'Entertainment', '#0000FF');

INSERT INTO transactions (uuid, name, amount, type, transaction_date) VALUES
                                                                          ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'Grocery Shopping', 150.00, 'EXPENSE', '2024-02-15'),
                                                                          ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'Movie Night', 50.00, 'EXPENSE', '2024-02-16'),
                                                                          ('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'Salary', 5000.00, 'INCOME', '2024-02-01');

INSERT INTO transaction_categories (transaction_id, category_uuid) VALUES
                                                                       (1, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'),  -- Grocery + Food
                                                                       (1, 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12'),  -- Grocery + Household
                                                                       (2, 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13'),  -- Movie + Entertainment
                                                                       (2, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11');  -- Movie + Food
