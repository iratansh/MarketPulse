package com.marketpulse;

public record MarketEvent(
    String ticker,
    double price,
    int volume,
    long timestamp
) {}
