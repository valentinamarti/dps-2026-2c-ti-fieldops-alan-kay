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
public class ActivityRow {
    @Column(name = "activity_id")
    private String activityId;
    @Column(name = "name")
    private String name;
    @Column(name = "window_start")
    private LocalDateTime windowStart;
    @Column(name = "window_end")
    private LocalDateTime windowEnd;
    @Column(name = "zone_id")
    private String zoneId;
    @Column(name = "type_name")
    private String typeName;
    @Column(name = "at_night")
    private boolean atNight;
}
