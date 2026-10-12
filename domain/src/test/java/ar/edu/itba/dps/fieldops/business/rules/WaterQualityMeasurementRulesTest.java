package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WaterQualityMeasurementRulesTest {

    @Test
    void durationIsCalibrationTimePlusFifteenMinutesPerStation() {
        assertEquals(Duration.ofMinutes(90), new WaterQualityMeasurementRules(4).estimatedDuration());
    }

    @Test
    void riskIsLow() {
        assertEquals(RiskLevel.LOW, new WaterQualityMeasurementRules(4).estimatedRisk());
    }

    @Test
    void requiresACertifiedWaterAnalyst() {
        final var analyst = new StaffRequirement(1, Set.of(new Certification("Water analysis")));

        assertEquals(List.of(analyst), new WaterQualityMeasurementRules(4).requiredStaff());
    }

    @Test
    void requiresAMultiparameterProbe() {
        final var probe = new ReusableRequirement(new ResourceCategory("Multiparameter probe"), 1);

        assertEquals(List.of(probe), new WaterQualityMeasurementRules(4).requiredEquipment());
    }

    @Test
    void consumesOneReagentKitPerStation() {
        final var kits = new DepletableRequirement(new ResourceCategory("Reagent kit"), Quantity.of("4", UNITS));

        assertEquals(List.of(kits), new WaterQualityMeasurementRules(4).requiredSupplies());
    }

    @Test
    void rejectsNonPositiveStationCount() {
        assertThrows(IllegalArgumentException.class, () -> new WaterQualityMeasurementRules(0));
    }
}
