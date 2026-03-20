CREATE TABLE IF NOT EXISTS Orders (
                        order_id        BIGINT PRIMARY KEY,
                        customer_id     BIGINT NOT NULL,
                        order_status    VARCHAR(50) NOT NULL,
                        total_amount    DECIMAL(10,2) NOT NULL,
                        currency        VARCHAR(10),
                        created_at      TIMESTAMP NOT NULL,
                        updated_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS OrderItems (
                            order_item_id   BIGINT PRIMARY KEY,
                            order_id        BIGINT NOT NULL,
                            product_id      BIGINT NOT NULL,
                            product_name    VARCHAR(255),
                            quantity        INT NOT NULL,
                            unit_price      DECIMAL(10,2) NOT NULL,
                            total_price     DECIMAL(10,2),
                            created_at      TIMESTAMP NOT NULL,
                            FOREIGN KEY (order_id) REFERENCES Orders(order_id)
);


CREATE TABLE IF NOT EXISTS OutboxEvents (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              aggregate_id BIGINT,
                              event_type VARCHAR(255),
                              payload TEXT,
                              status VARCHAR(50),
                              created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_customer ON Orders(customer_id);