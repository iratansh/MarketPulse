package com.marketpulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class ProducerApp {
  public static void main(String[] args) throws Exception {
    Config config = Config.fromEnv();

    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.ACKS_CONFIG, "1");
    props.put(ProducerConfig.LINGER_MS_CONFIG, "5");
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, 64 * 1024);
    props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

    ObjectMapper mapper = new ObjectMapper();
    MarketEventGenerator generator = new MarketEventGenerator(config.tickers);

    AtomicLong sent = new AtomicLong();
    long tickIntervalMs = 100;
    int ticksPerSecond = (int) (1000 / tickIntervalMs);
    int perTick = Math.max(1, config.targetMessagesPerSecond / ticksPerSecond);

    try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
      ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

      Runnable task = () -> {
        for (int i = 0; i < perTick; i++) {
          MarketEvent event = generator.next();
          try {
            String json = mapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(config.topic, event.ticker(), json);
            producer.send(record, (RecordMetadata md, Exception ex) -> {
              if (ex == null) {
                sent.incrementAndGet();
              }
            });
          } catch (Exception ex) {
            System.err.println("Failed to serialize event: " + ex.getMessage());
          }
        }
      };

      scheduler.scheduleAtFixedRate(task, 0, tickIntervalMs, TimeUnit.MILLISECONDS);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        scheduler.shutdown();
        try {
          scheduler.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
        producer.flush();
      }));

      long last = System.nanoTime();
      while (true) {
        Thread.sleep(1000);
        long now = System.nanoTime();
        long count = sent.getAndSet(0);
        double seconds = Duration.ofNanos(now - last).toMillis() / 1000.0;
        last = now;
        System.out.printf("sent %.0f msg/sec\n", count / seconds);
      }
    }
  }
}
