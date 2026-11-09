CREATE TABLE notification (
    sequence       BIGSERIAL PRIMARY KEY,
    event_id       VARCHAR(255) NOT NULL,
    expedition_id  VARCHAR(255) NOT NULL,
    responsible_id VARCHAR(255) NOT NULL,
    text           TEXT         NOT NULL,
    occurred_at    TIMESTAMP(6) NOT NULL,
    UNIQUE (event_id, responsible_id)
);

CREATE INDEX notification_of_expedition ON notification (expedition_id, sequence);
