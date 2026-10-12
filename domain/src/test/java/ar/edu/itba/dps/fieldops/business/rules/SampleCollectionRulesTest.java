package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SampleCollectionRulesTest {

    @Test
    void durationIsSetupTimePlusTwentyMinutesPerSample() {
        assertEquals(Duration.ofHours(2), new SampleCollectionRules(3).estimatedDuration());
    }

    @Test
    void riskIsLow() {
        assertEquals(RiskLevel.LOW, new SampleCollectionRules(3).estimatedRisk());
    }

    @Test
    void requiresACoolerToKeepTheSamples() {
        final var cooler = new ReusableRequirement(new ResourceCategory("Cooler"), 1);

        assertEquals(List.of(cooler), new SampleCollectionRules(3).requiredEquipment());
    }

    @Test
    void consumesOneContainerPerSample() {
        final var containers = new DepletableRequirement(new ResourceCategory("Sample container"), Quantity.of("3", UNITS));

        assertEquals(List.of(containers), new SampleCollectionRules(3).requiredSupplies());
    }

    @Test
    void requiresOneFieldWorkerWithoutSpecificCertifications() {
        assertEquals(List.of(new StaffRequirement(1, Set.of())), new SampleCollectionRules(3).requiredStaff());
    }

    @Test
    void rejectsNonPositiveSampleCount() {
        assertThrows(IllegalArgumentException.class, () -> new SampleCollectionRules(0));
    }
}
