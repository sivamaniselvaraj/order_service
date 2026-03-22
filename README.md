# Order Service

## API
POST /orders
GET /orders{id}

## Description
Creates order and publishes event to Kafka topic `order_topic`


## Flow
Order Service -> Saves order -> Publishes event(OrderCreated)

## Run
mvn clean package
docker build -t order-service .
docker run -p 8081:8081 order-service