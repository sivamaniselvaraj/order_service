CREATE TABLE IF NOT EXISTS ORDERS (
                        id              BIGINT PRIMARY KEY,
                        order_id        VARCHAR(150),
                        correlation_id  VARCHAR(150) NOT NULL,
                        customer_id     VARCHAR(50) NOT NULL,
                        customer_email  VARCHAR(150) NOT NULL,
                        order_status    VARCHAR(50) NOT NULL,
                        total_amount    DECIMAL(10,2) NOT NULL,
                        currency        VARCHAR(10),
                        idempotency_key VARCHAR(100),
                        created_at      TIMESTAMP NOT NULL,
                        updated_at      TIMESTAMP,
                        CONSTRAINT (unique_idempotency) UNIQUE (idempotency_key)
                        CONSTRAINT (unique_order) UNIQUE (order_id)
                        CONSTRAINT (unique_correlation) UNIQUE (correlation_id)

);

CREATE TABLE IF NOT EXISTS ORDER_ITEMS (
                            id   BIGINT PRIMARY KEY,
                            order_id        BIGINT NOT NULL,
                            product_id      BIGINT NOT NULL,
                            product_name    VARCHAR(255),
                            quantity        INT NOT NULL,
                            unit_price      DECIMAL(10,2) NOT NULL,
                            total_price     DECIMAL(10,2),
                            created_at      TIMESTAMP NOT NULL,
                            FOREIGN KEY (order_id) REFERENCES Orders(order_id)
);


CREATE TABLE IF NOT EXISTS outbox_messages (
                            id              BIGINT PRIMARY KEY,
                            aggregate_type  VARCHAR(100),
                            aggregate_id    VARCHAR(100),
                            correlation_id  VARCHAR(100),
                            event_type      VARCHAR(100),
                            destination     VARCHAR(100),
                            payload         JSON,
                            status          VARCHAR(50),
                            retry_count     BIGINT,
                            max_retries     BIGINT,
                            last_attempt_at TIMESTAMP,
                            last_error      TEXT,
                            created_at      TIMESTAMP,
                            published_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_customer ON Orders(customer_id);