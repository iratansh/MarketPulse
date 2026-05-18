# MarketPulse Flink Processor

Flink stream processing application that calculates Volume-Weighted Average Price (VWAP) for stock tickers.

## Build

```bash
cd flink-processor
mvn clean package -DskipTests
```

## Run

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
KAFKA_TOPIC=market.ticks \
JDBC_URL=jdbc:postgresql://localhost:5432/marketpulse \
JDBC_USER=postgres \
JDBC_PASSWORD=postgres \
WINDOW_SECONDS=5 \
java -jar target/marketpulse-flink-0.1.0.jar
```

## Environment Variables

- `KAFKA_BOOTSTRAP_SERVERS` - Kafka broker address (default: `localhost:9092`)
- `KAFKA_TOPIC` - Kafka topic to consume from (default: `market.ticks`)
- `JDBC_URL` - TimescaleDB connection URL (default: `jdbc:postgresql://localhost:5432/marketpulse`)
- `JDBC_USER` - Database user (default: `postgres`)
- `JDBC_PASSWORD` - Database password (default: `postgres`)
- `WINDOW_SECONDS` - Tumbling window size in seconds (default: `5`)

## VWAP Calculation

The processor calculates VWAP using tumbling windows:

```
VWAP = Σ(price × volume) / Σ(volume)
```

For each ticker, every N seconds (configurable via `WINDOW_SECONDS`), the aggregated VWAP is calculated and written to TimescaleDB.
