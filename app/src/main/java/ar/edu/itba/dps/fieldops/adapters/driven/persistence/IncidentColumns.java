package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IncidentColumns {
    @Column(name = "incident_expedition")
    private String expedition;
    @Column(name = "incident_number")
    private int number;
    @Column(name = "incident_activity")
    private String activity;
    @Column(name = "incident_description")
    private String description;
    @Column(name = "incident_occurred_at")
    private LocalDateTime occurredAt;
    @Column(name = "incident_resource")
    private String resource;
    @Column(name = "incident_delay_minutes")
    private Long delayMinutes;
}
