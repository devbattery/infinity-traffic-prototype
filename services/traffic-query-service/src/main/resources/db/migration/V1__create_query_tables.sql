CREATE TABLE IF NOT EXISTS traffic_event (
    event_id VARCHAR(64) PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL,
    region VARCHAR(100) NOT NULL,
    road_name VARCHAR(200) NOT NULL,
    average_speed_kph INTEGER NOT NULL,
    congestion_level INTEGER NOT NULL,
    observed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_traffic_event_observed_at ON traffic_event (observed_at DESC);
CREATE INDEX IF NOT EXISTS idx_traffic_event_region ON traffic_event (region);

CREATE TABLE IF NOT EXISTS traffic_region_projection (
    region VARCHAR(100) PRIMARY KEY,
    total_events BIGINT NOT NULL,
    speed_sum BIGINT NOT NULL,
    latest_congestion_level INTEGER NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
