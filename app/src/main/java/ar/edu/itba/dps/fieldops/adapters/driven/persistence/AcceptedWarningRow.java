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
public class AcceptedWarningRow {
    @Column(name = "severity")
    private String severity;
    @Column(name = "rule_id")
    private String rule;
    @Column(name = "subject_activity")
    private String subjectActivity;
    @Column(name = "subject_resource")
    private String subjectResource;
    @Column(name = "message")
    private String message;
    @Column(name = "responsible_id")
    private String responsibleId;
    @Column(name = "justification")
    private String justification;
}
