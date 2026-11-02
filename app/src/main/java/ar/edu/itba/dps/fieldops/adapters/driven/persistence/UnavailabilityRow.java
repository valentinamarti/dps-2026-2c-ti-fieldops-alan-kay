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
public class UnavailabilityRow {
    @Column(name = "start_at")
    private LocalDateTime startAt;
    @Column(name = "end_at")
    private LocalDateTime endAt;
    @Column(name = "reason")
    private String reason;
}
