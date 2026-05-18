package com.marketpulse;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class Config {
  public final String bootstrapServers;
  public final String topic;
  public final List<String> tickers;
  public final int targetMessagesPerSecond;

  private Config(
      String bootstrapServers,
      String topic,
      List<String> tickers,
      int targetMessagesPerSecond
  ) {
    this.bootstrapServers = bootstrapServers;
    this.topic = topic;
    this.tickers = tickers;
    this.targetMessagesPerSecond = targetMessagesPerSecond;
  }

  public static Config fromEnv() {
    String bootstrap = getEnv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    String topic = getEnv("KAFKA_TOPIC", "market.ticks");
    String tickersRaw = getEnv("TICKERS", "AAPL,MSFT,NVDA,AMZN,TSLA,META,GOOGL");
    int target = getEnvInt("TARGET_MSGS_PER_SEC", 10000);

    List<String> tickers = List.of(tickersRaw.split(","))
        .stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(s -> s.toUpperCase(Locale.ROOT))
        .toList();

    if (target < 1) {
      throw new IllegalArgumentException("TARGET_MSGS_PER_SEC must be >= 1");
    }

    return new Config(bootstrap, topic, tickers, target);
  }

  private static String getEnv(String key, String defaultValue) {
    return Optional.ofNullable(System.getenv(key)).orElse(defaultValue);
  }

  private static int getEnvInt(String key, int defaultValue) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Integer.parseInt(value);
  }
}
