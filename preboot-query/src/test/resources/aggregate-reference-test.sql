CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            uuid UUID NOT NULL UNIQUE,
                            name VARCHAR(255) NOT NULL,
                            description TEXT
);

CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          uuid UUID NOT NULL UNIQUE,
                          name VARCHAR(255) NOT NULL,
                          price DECIMAL(19,2) NOT NULL,
                          category_uuid UUID NOT NULL,
                          FOREIGN KEY (category_uuid) REFERENCES categories(uuid)
);

-- Insert test categories
INSERT INTO categories (uuid, name, description) VALUES
                                                     ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Electronics', 'Electronic devices and accessories'),
                                                     ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'Books', 'Physical and digital books'),
                                                     ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'Clothing', 'Apparel and accessories');

-- Insert test products
INSERT INTO products (uuid, name, price, category_uuid) VALUES
                                                            ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'Smartphone', 599.99, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'),
                                                            ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'Laptop', 1299.99, 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'),
                                                            ('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'Programming Book', 49.99, 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12'),
                                                            ('70eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'T-Shirt', 29.99, 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13');
