package com.marketpulse.flink;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.sql.Timestamp;

public class FlinkProcessor {

    public static void main(String[] args) throws Exception {
        // Configuration from environment variables
        String kafkaBootstrap = getEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String kafkaTopic = getEnv("KAFKA_TOPIC", "market.ticks");
        String jdbcUrl = getEnv("JDBC_URL", "jdbc:postgresql://localhost:5432/marketpulse");
        String jdbcUser = getEnv("JDBC_USER", "postgres");
        String jdbcPassword = getEnv("JDBC_PASSWORD", "postgres");
        int windowSeconds = Integer.parseInt(getEnv("WINDOW_SECONDS", "5"));

        // Create execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // Kafka source
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(kafkaBootstrap)
                .setTopics(kafkaTopic)
                .setGroupId("flink-vwap-processor")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // ObjectMapper for JSON deserialization
        ObjectMapper mapper = new ObjectMapper();

        // Stream pipeline
        DataStream<String> rawStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        DataStream<MarketEvent> eventStream = rawStream
                .map(json -> mapper.readValue(json, MarketEvent.class))
                .name("Parse JSON");

        DataStream<VwapResult> vwapStream = eventStream
                .keyBy(event -> event.ticker)
                .window(TumblingProcessingTimeWindows.of(Time.seconds(windowSeconds)))
                .aggregate(new VwapAggregator())
                .name("Calculate VWAP");

        // Print to console (for debugging)
        vwapStream.print();

        // Sink to TimescaleDB
        vwapStream.addSink(
                JdbcSink.sink(
                        "INSERT INTO vwap_metrics (ticker, vwap, total_volume, window_start, window_end) VALUES (?, ?, ?, ?, ?)",
                        (JdbcStatementBuilder<VwapResult>) (statement, result) -> {
                            statement.setString(1, result.ticker);
                            statement.setDouble(2, result.vwap);
                            statement.setLong(3, result.totalVolume);
                            statement.setTimestamp(4, result.windowStart);
                            statement.setTimestamp(5, result.windowEnd);
                        },
                        JdbcExecutionOptions.builder()
                                .withBatchSize(100)
                                .withBatchIntervalMs(1000)
                                .withMaxRetries(3)
                                .build(),
                        new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                                .withUrl(jdbcUrl)
                                .withDriverName("org.postgresql.Driver")
                                .withUsername(jdbcUser)
                                .withPassword(jdbcPassword)
                                .build()
                )
        ).name("TimescaleDB Sink");

        env.execute("MarketPulse VWAP Processor");
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /**
     * Aggregate function to calculate Volume-Weighted Average Price (VWAP)
     */
    public static class VwapAggregator implements AggregateFunction<MarketEvent, VwapAccumulator, VwapResult> {

        @Override
        public VwapAccumulator createAccumulator() {
            return new VwapAccumulator();
        }

        @Override
        public VwapAccumulator add(MarketEvent event, VwapAccumulator acc) {
            acc.ticker = event.ticker;
            acc.priceVolumeSum += event.price * event.volume;
            acc.volumeSum += event.volume;
            return acc;
        }

        @Override
        public VwapResult getResult(VwapAccumulator acc) {
            double vwap = acc.volumeSum > 0 ? acc.priceVolumeSum / acc.volumeSum : 0.0;
            long now = System.currentTimeMillis();
            return new VwapResult(
                    acc.ticker,
                    vwap,
                    acc.volumeSum,
                    new Timestamp(now),
                    new Timestamp(now)
            );
        }

        @Override
        public VwapAccumulator merge(VwapAccumulator a, VwapAccumulator b) {
            a.priceVolumeSum += b.priceVolumeSum;
            a.volumeSum += b.volumeSum;
            return a;
        }
    }

    public static class VwapAccumulator {
        public String ticker;
        public double priceVolumeSum = 0.0;
        public long volumeSum = 0;
    }
}
