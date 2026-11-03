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
public class NodeAssignmentRow {
    @Column(name = "activity_id")
    private String activityId;
    @Column(name = "role")
    private String role;
    @Column(name = "resource_id")
    private String resourceId;
}
