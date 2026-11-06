CREATE TABLE domain_event (
    sequence      BIGSERIAL PRIMARY KEY,
    event_id      VARCHAR(255) NOT NULL UNIQUE,
    expedition_id VARCHAR(255) NOT NULL,
    event_type    VARCHAR(255) NOT NULL,
    occurred_at   TIMESTAMP(6) NOT NULL,
    recorded_at   TIMESTAMP(6) NOT NULL,
    payload       JSONB        NOT NULL
);

CREATE INDEX domain_event_of_expedition ON domain_event (expedition_id, sequence);

CREATE TABLE event_delivery (
    event_sequence  BIGINT       NOT NULL REFERENCES domain_event (sequence) ON DELETE CASCADE,
    consumer        VARCHAR(255) NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    attempts        INTEGER      NOT NULL,
    last_error      VARCHAR(1000),
    next_attempt_at TIMESTAMP(6) NOT NULL,
    delivered_at    TIMESTAMP(6),
    PRIMARY KEY (event_sequence, consumer)
);

CREATE INDEX event_delivery_to_attempt ON event_delivery (status, next_attempt_at);
