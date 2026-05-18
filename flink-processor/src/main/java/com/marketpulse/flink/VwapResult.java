package com.marketpulse.flink;

import java.sql.Timestamp;

public class VwapResult {
    public String ticker;
    public double vwap;
    public long totalVolume;
    public Timestamp windowStart;
    public Timestamp windowEnd;

    public VwapResult() {}

    public VwapResult(String ticker, double vwap, long totalVolume, Timestamp windowStart, Timestamp windowEnd) {
        this.ticker = ticker;
        this.vwap = vwap;
        this.totalVolume = totalVolume;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
    }

    @Override
    public String toString() {
        return String.format("VwapResult{ticker='%s', vwap=%.2f, totalVolume=%d, window=[%s, %s]}",
                ticker, vwap, totalVolume, windowStart, windowEnd);
    }
}
