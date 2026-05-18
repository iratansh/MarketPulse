-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Create the VWAP metrics table
CREATE TABLE IF NOT EXISTS vwap_metrics (
    time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ticker TEXT NOT NULL,
    vwap DOUBLE PRECISION NOT NULL,
    total_volume BIGINT NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL
);

-- Convert to hypertable (TimescaleDB time-series optimization)
SELECT create_hypertable('vwap_metrics', 'time', if_not_exists => TRUE);

-- Create index on ticker for faster queries
CREATE INDEX IF NOT EXISTS idx_ticker_time ON vwap_metrics (ticker, time DESC);

-- Create continuous aggregate for 1-minute rollups (optional, for performance)
CREATE MATERIALIZED VIEW IF NOT EXISTS vwap_1min
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', time) AS bucket,
    ticker,
    AVG(vwap) AS avg_vwap,
    SUM(total_volume) AS total_volume
FROM vwap_metrics
GROUP BY bucket, ticker
WITH NO DATA;

-- Refresh policy for the continuous aggregate
SELECT add_continuous_aggregate_policy('vwap_1min',
    start_offset => INTERVAL '1 hour',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute',
    if_not_exists => TRUE
);
