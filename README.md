# MarketPulse

MarketPulse is a streaming market simulation and analytics stack. A Java producer generates live stock ticks, Kafka transports the stream, Flink computes rolling VWAP, TimescaleDB stores metrics, and Grafana visualizes the results.

## Architecture

```mermaid
flowchart LR
	Producer[Java Producer\nMarketPulse] -->|JSON ticks| Kafka[(Kafka)]
	Kafka -->|stream| Flink[Flink VWAP Processor]
	Flink -->|aggregates| Timescale[(TimescaleDB)]
	Timescale -->|queries| Grafana[Grafana]

	subgraph Local_or_EC2[Runtime Host]
		Kafka
		Flink
		Timescale
		Grafana
	end

	Terraform[Terraform\nAWS IaC] -.->|provisions| EC2[(EC2 + Docker Compose)]
	EC2 -.-> Local_or_EC2
```

## Data Flow

```mermaid
sequenceDiagram
	participant P as Producer
	participant K as Kafka
	participant F as Flink
	participant T as TimescaleDB
	participant G as Grafana

	P->>K: emit JSON ticks
	K->>F: consume market.ticks
	F->>T: write VWAP window results
	G->>T: query metrics
```

## Entity Relationship Diagram

The database schema lives in [init-db.sql](init-db.sql). The diagram shows core tables and the continuous aggregate view derived from them.

```mermaid
erDiagram
	vwap_metrics {
		timestamptz time
		text ticker
		double vwap
		bigint total_volume
		timestamptz window_start
		timestamptz window_end
	}

	vwap_1min {
		timestamptz bucket
		text ticker
		double avg_vwap
		bigint total_volume
	}

	vwap_metrics ||--o{ vwap_1min : aggregates
```

## Components

- Java producer: [src/main/java/com/marketpulse/ProducerApp.java](src/main/java/com/marketpulse/ProducerApp.java)
- Flink processor: [flink-processor/src/main/java/com/marketpulse/flink/FlinkProcessor.java](flink-processor/src/main/java/com/marketpulse/flink/FlinkProcessor.java)
- Local stack (Kafka, TimescaleDB, Grafana): [docker-compose.yml](docker-compose.yml)
- Infrastructure as Code (AWS): [terraform/README.md](terraform/README.md)

## Technologies Used

- Java 17
- Apache Kafka
- Apache Flink
- TimescaleDB (PostgreSQL)
- Grafana
- Docker Compose
- Terraform (AWS provisioning)

## Notes

- The Flink processor calculates VWAP over tumbling windows and persists results to TimescaleDB.
- The Terraform stack provisions an EC2 host and runs the same containerized services.
