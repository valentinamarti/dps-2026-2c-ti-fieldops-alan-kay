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
public class ReasonColumns {
    @Column(name = "reason_kind")
    private String kind;
    @Column(name = "reason_delay_minutes")
    private Long delayMinutes;
    @Column(name = "reason_withdrawn")
    private String withdrawn;
    @Column(name = "reason_replacement")
    private String replacement;
}
