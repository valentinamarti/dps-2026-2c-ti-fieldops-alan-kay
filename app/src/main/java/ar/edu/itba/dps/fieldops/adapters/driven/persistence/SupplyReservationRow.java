package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SupplyReservationRow {
    @Column(name = "expedition_id")
    private String expeditionId;
    @Column(name = "amount")
    private BigDecimal amount;
    @Column(name = "unit")
    private String unit;
}
