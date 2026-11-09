package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "expedition_progress")
@Getter
@Setter
public class ProgressEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sequence;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "expedition_id")
    private String expeditionId;

    @Column(name = "activity_id")
    private String activityId;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "finished_activities")
    private int finishedActivities;

    @Column(name = "scheduled_activities")
    private int scheduledActivities;
}
