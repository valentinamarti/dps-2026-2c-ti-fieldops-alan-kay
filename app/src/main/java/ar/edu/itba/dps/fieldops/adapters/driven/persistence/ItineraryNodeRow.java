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
public class ItineraryNodeRow {
    @Column(name = "parent_position")
    private Integer parentPosition;
    @Column(name = "kind")
    private String kind;
    @Column(name = "activity_id")
    private String activityId;
    @Column(name = "start_at")
    private LocalDateTime startAt;
    @Column(name = "end_at")
    private LocalDateTime endAt;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    @Column(name = "result")
    private String result;
}
