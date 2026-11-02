CREATE TABLE zone (
    id            VARCHAR(255) PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    created_order BIGSERIAL NOT NULL
);

CREATE TABLE zone_permit (
    zone_id VARCHAR(255) NOT NULL REFERENCES zone (id) ON DELETE CASCADE,
    permit  VARCHAR(255) NOT NULL,
    PRIMARY KEY (zone_id, permit)
);

CREATE TABLE resource (
    id            VARCHAR(255) PRIMARY KEY,
    kind          VARCHAR(32)  NOT NULL,
    name          VARCHAR(255) NOT NULL,
    category      VARCHAR(255),
    license_plate VARCHAR(255),
    stock_amount  NUMERIC(19, 4),
    stock_unit    VARCHAR(32),
    version       BIGINT       NOT NULL,
    created_order BIGSERIAL    NOT NULL
);

CREATE TABLE resource_certification (
    resource_id   VARCHAR(255) NOT NULL REFERENCES resource (id) ON DELETE CASCADE,
    certification VARCHAR(255) NOT NULL,
    PRIMARY KEY (resource_id, certification)
);

CREATE TABLE resource_reservation (
    resource_id   VARCHAR(255) NOT NULL REFERENCES resource (id) ON DELETE CASCADE,
    sort_order    INTEGER      NOT NULL,
    expedition_id VARCHAR(255) NOT NULL,
    start_at      TIMESTAMP(6) NOT NULL,
    end_at        TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (resource_id, sort_order)
);

CREATE TABLE resource_unavailability (
    resource_id VARCHAR(255) NOT NULL REFERENCES resource (id) ON DELETE CASCADE,
    sort_order  INTEGER      NOT NULL,
    start_at    TIMESTAMP(6) NOT NULL,
    end_at      TIMESTAMP(6) NOT NULL,
    reason      VARCHAR(255) NOT NULL,
    PRIMARY KEY (resource_id, sort_order)
);

CREATE TABLE supply_reservation (
    resource_id   VARCHAR(255)   NOT NULL REFERENCES resource (id) ON DELETE CASCADE,
    sort_order    INTEGER        NOT NULL,
    expedition_id VARCHAR(255)   NOT NULL,
    amount        NUMERIC(19, 4) NOT NULL,
    unit          VARCHAR(32)    NOT NULL,
    PRIMARY KEY (resource_id, sort_order)
);

CREATE TABLE expedition (
    id                     VARCHAR(255) PRIMARY KEY,
    name                   VARCHAR(255) NOT NULL,
    period_start           TIMESTAMP(6) NOT NULL,
    period_end             TIMESTAMP(6) NOT NULL,
    max_participants       INTEGER      NOT NULL,
    risk_tolerance         VARCHAR(32)  NOT NULL,
    status                 VARCHAR(32)  NOT NULL,
    replaced_expedition    VARCHAR(255),
    replaced_revision      INTEGER,
    reason_kind            VARCHAR(32),
    reason_delay_minutes   BIGINT,
    reason_withdrawn       VARCHAR(255),
    reason_replacement     VARCHAR(255),
    replacement_expedition VARCHAR(255),
    execution_revision     INTEGER      NOT NULL,
    version                BIGINT       NOT NULL,
    created_order          BIGSERIAL    NOT NULL
);

CREATE TABLE expedition_objective (
    expedition_id VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    sort_order    INTEGER      NOT NULL,
    objective     VARCHAR(1000) NOT NULL,
    PRIMARY KEY (expedition_id, sort_order)
);

CREATE TABLE expedition_zone (
    expedition_id VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    zone_id       VARCHAR(255) NOT NULL,
    PRIMARY KEY (expedition_id, zone_id)
);

CREATE TABLE expedition_responsible (
    expedition_id VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    person_id     VARCHAR(255) NOT NULL,
    PRIMARY KEY (expedition_id, person_id)
);

CREATE TABLE expedition_permit (
    expedition_id VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    permit        VARCHAR(255) NOT NULL,
    PRIMARY KEY (expedition_id, permit)
);

CREATE TABLE accepted_warning (
    expedition_id    VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    sort_order       INTEGER      NOT NULL,
    severity         VARCHAR(32)  NOT NULL,
    rule_id          VARCHAR(255) NOT NULL,
    subject_activity VARCHAR(255),
    subject_resource VARCHAR(255),
    message          VARCHAR(1000) NOT NULL,
    responsible_id   VARCHAR(255) NOT NULL,
    justification    VARCHAR(1000) NOT NULL,
    PRIMARY KEY (expedition_id, sort_order)
);

CREATE TABLE expedition_incident (
    expedition_id       VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    sort_order          INTEGER      NOT NULL,
    incident_expedition VARCHAR(255) NOT NULL,
    incident_number     INTEGER      NOT NULL,
    incident_activity   VARCHAR(255) NOT NULL,
    incident_description VARCHAR(1000) NOT NULL,
    incident_occurred_at TIMESTAMP(6) NOT NULL,
    incident_resource   VARCHAR(255),
    incident_delay_minutes BIGINT,
    PRIMARY KEY (expedition_id, sort_order)
);

CREATE TABLE expedition_activity (
    expedition_id VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    sort_order    INTEGER      NOT NULL,
    activity_id   VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    window_start  TIMESTAMP(6) NOT NULL,
    window_end    TIMESTAMP(6) NOT NULL,
    zone_id       VARCHAR(255) NOT NULL,
    type_name     VARCHAR(255) NOT NULL,
    at_night      BOOLEAN      NOT NULL,
    PRIMARY KEY (expedition_id, sort_order)
);

CREATE TABLE activity_parameter (
    expedition_id VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    sort_order    INTEGER      NOT NULL,
    activity_id   VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    value         INTEGER      NOT NULL,
    PRIMARY KEY (expedition_id, sort_order)
);

CREATE TABLE activity_dependency (
    expedition_id VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    sort_order    INTEGER      NOT NULL,
    activity_id   VARCHAR(255) NOT NULL,
    depends_on    VARCHAR(255) NOT NULL,
    PRIMARY KEY (expedition_id, sort_order)
);

CREATE TABLE itinerary_node (
    expedition_id   VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    sort_order      INTEGER      NOT NULL,
    parent_position INTEGER,
    kind            VARCHAR(32)  NOT NULL,
    activity_id     VARCHAR(255),
    start_at        TIMESTAMP(6),
    end_at          TIMESTAMP(6),
    started_at      TIMESTAMP(6),
    finished_at     TIMESTAMP(6),
    result          VARCHAR(1000),
    PRIMARY KEY (expedition_id, sort_order)
);

CREATE TABLE node_assignment (
    expedition_id VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    sort_order    INTEGER      NOT NULL,
    activity_id   VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    resource_id   VARCHAR(255) NOT NULL,
    PRIMARY KEY (expedition_id, sort_order)
);

CREATE TABLE node_observation (
    expedition_id VARCHAR(255) NOT NULL REFERENCES expedition (id) ON DELETE CASCADE,
    sort_order    INTEGER      NOT NULL,
    activity_id   VARCHAR(255) NOT NULL,
    note          VARCHAR(1000) NOT NULL,
    recorded_at   TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (expedition_id, sort_order)
);

CREATE TABLE replan_proposal (
    id                    VARCHAR(255) PRIMARY KEY,
    expedition_id         VARCHAR(255) NOT NULL,
    status                VARCHAR(32)  NOT NULL,
    incident_expedition   VARCHAR(255) NOT NULL,
    incident_number       INTEGER      NOT NULL,
    incident_activity     VARCHAR(255) NOT NULL,
    incident_description  VARCHAR(1000) NOT NULL,
    incident_occurred_at  TIMESTAMP(6) NOT NULL,
    incident_resource     VARCHAR(255),
    incident_delay_minutes BIGINT,
    responsible_id        VARCHAR(255),
    justification         VARCHAR(1000),
    decided_at            TIMESTAMP(6),
    chosen_option         INTEGER,
    alternative_expedition VARCHAR(255),
    superseded_by         VARCHAR(255),
    version               BIGINT       NOT NULL,
    created_order         BIGSERIAL    NOT NULL
);

CREATE TABLE replan_option (
    proposal_id          VARCHAR(255) NOT NULL REFERENCES replan_proposal (id) ON DELETE CASCADE,
    sort_order           INTEGER      NOT NULL,
    summary              VARCHAR(1000) NOT NULL,
    reason_kind          VARCHAR(32),
    reason_delay_minutes BIGINT,
    reason_withdrawn     VARCHAR(255),
    reason_replacement   VARCHAR(255),
    PRIMARY KEY (proposal_id, sort_order)
);
