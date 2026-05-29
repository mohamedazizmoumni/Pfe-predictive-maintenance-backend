-- Create core inventory tables required by inventory-module entities

CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS parts (
    id BIGSERIAL PRIMARY KEY,
    part_number VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(100),
    cost NUMERIC(19,4),
    current_stock INTEGER NOT NULL,
    minimum_stock INTEGER NOT NULL,
    reorder_quantity INTEGER,
    unit VARCHAR(50),
    supplier VARCHAR(255),
    status VARCHAR(255),
    notes VARCHAR(1000),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS reorder_requests (
    id BIGSERIAL PRIMARY KEY,
    part_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(255) NOT NULL,
    reason VARCHAR(100),
    requested_by VARCHAR(100),
    approved_by VARCHAR(100),
    notes VARCHAR(1000),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_date TIMESTAMP,
    version BIGINT,
    CONSTRAINT fk_reorder_requests_part FOREIGN KEY (part_id) REFERENCES parts(id)
);

CREATE TABLE IF NOT EXISTS stock_orders (
    id BIGSERIAL PRIMARY KEY,
    reorder_request_id BIGINT NOT NULL,
    part_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    cost NUMERIC(19,4),
    status VARCHAR(255) NOT NULL,
    supplier_purchase_order VARCHAR(100),
    expected_delivery_date VARCHAR(200),
    delivered_date VARCHAR(200),
    ordered_by VARCHAR(100),
    proof_of_delivery VARCHAR(1000),
    notes VARCHAR(1000),
    ordered_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP,
    version BIGINT,
    CONSTRAINT fk_stock_orders_reorder_request FOREIGN KEY (reorder_request_id) REFERENCES reorder_requests(id),
    CONSTRAINT fk_stock_orders_part FOREIGN KEY (part_id) REFERENCES parts(id),
    CONSTRAINT uk_stock_orders_reorder_request UNIQUE (reorder_request_id)
);

CREATE INDEX IF NOT EXISTS idx_reorder_requests_part_id ON reorder_requests(part_id);
CREATE INDEX IF NOT EXISTS idx_stock_orders_part_id ON stock_orders(part_id);
