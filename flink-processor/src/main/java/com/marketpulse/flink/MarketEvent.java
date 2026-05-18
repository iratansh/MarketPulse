package com.marketpulse.flink;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MarketEvent {
    @JsonProperty("ticker")
    public String ticker;

    @JsonProperty("price")
    public double price;

    @JsonProperty("volume")
    public int volume;

    @JsonProperty("timestamp")
    public long timestamp;

    public MarketEvent() {}

    public MarketEvent(String ticker, double price, int volume, long timestamp) {
        this.ticker = ticker;
        this.price = price;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("MarketEvent{ticker='%s', price=%.2f, volume=%d, timestamp=%d}",
                ticker, price, volume, timestamp);
    }
}
