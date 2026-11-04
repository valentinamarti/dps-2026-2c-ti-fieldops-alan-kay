package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "domain_event")
@Getter
@Setter
public class DomainEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sequence;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "expedition_id")
    private String expeditionId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;
}
