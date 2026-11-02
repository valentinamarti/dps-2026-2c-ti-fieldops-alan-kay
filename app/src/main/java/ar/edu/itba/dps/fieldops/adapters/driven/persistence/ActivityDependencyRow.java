package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDependencyRow {
    @Column(name = "activity_id")
    private String activityId;
    @Column(name = "depends_on")
    private String dependsOn;
}
