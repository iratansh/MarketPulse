# MarketPulse Producer

Java producer that simulates a live stock market stream and publishes JSON events to Kafka.

## Run

```bash
mvn -q -DskipTests package
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
KAFKA_TOPIC=market.ticks \
TARGET_MSGS_PER_SEC=10000 \
TICKERS=AAPL,MSFT,NVDA,AMZN,TSLA,META,GOOGL \
java -jar target/marketpulse-producer-0.1.0.jar
```

## Environment variables

- `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`)
- `KAFKA_TOPIC` (default `market.ticks`)
- `TARGET_MSGS_PER_SEC` (default `10000`)
- `TICKERS` (default `AAPL,MSFT,NVDA,AMZN,TSLA,META,GOOGL`)
