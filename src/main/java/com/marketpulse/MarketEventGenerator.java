package com.marketpulse;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MarketEventGenerator {
  private final List<String> tickers;
  private final ThreadLocalRandom random = ThreadLocalRandom.current();

  public MarketEventGenerator(List<String> tickers) {
    this.tickers = tickers;
  }

  public MarketEvent next() {
    String ticker = tickers.get(random.nextInt(tickers.size()));
    double price = round2(50 + random.nextDouble() * 300);
    int volume = random.nextInt(1, 2000);
    long timestamp = Instant.now().getEpochSecond();

    return new MarketEvent(ticker, price, volume, timestamp);
  }

  private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
