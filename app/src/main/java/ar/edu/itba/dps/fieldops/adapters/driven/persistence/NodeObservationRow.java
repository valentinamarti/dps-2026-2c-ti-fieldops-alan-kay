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
public class NodeObservationRow {
    @Column(name = "activity_id")
    private String activityId;
    @Column(name = "note")
    private String note;
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
}
