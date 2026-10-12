package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NightOperationRulesTest {

    private static final Certification NIGHT_OPERATIONS = new Certification("Night operations");
    private static final Certification FIRST_AID = new Certification("First aid");
    private static final ReusableRequirement LIGHTING = new ReusableRequirement(new ResourceCategory("Lighting"), 1);
    private static final Zone REEF = new Zone("z-1", "Reef", Set.of());
    private static final Permit NIGHT_PERMIT_OF_THE_REEF = new Permit("night operations in zone z-1");
    private static final Permit RESEARCH_PERMIT = new Permit("Research in protected waters");

    private static Stream<Arguments> baseRulesOfEveryType() {
        return Stream.of(
                Arguments.of("sample collection", new SampleCollectionRules(3)),
                Arguments.of("diving", new DivingRules(10)),
                Arguments.of("drone survey", new DroneSurveyRules(20)),
                Arguments.of("wildlife observation", new WildlifeObservationRules(2)),
                Arguments.of("water quality measurement", new WaterQualityMeasurementRules(4)),
                Arguments.of("transfer", new TransferRules(60)));
    }

    private static List<ReusableRequirement> withLighting(List<ReusableRequirement> equipment) {
        final var expected = new ArrayList<>(equipment);
        expected.add(LIGHTING);
        return expected;
    }

    @ParameterizedTest(name = "a night {0}")
    @MethodSource("baseRulesOfEveryType")
    void addsTheCertificationTheLightingAndThePermitAndKeepsTheDurationAndTheSupplies(String type, ActivityRules base) {
        final var night = new NightOperationRules(base);

        assertEquals(Set.of(NIGHT_OPERATIONS), night.requiredCertificationsForAllStaff());
        assertEquals(withLighting(base.requiredEquipment()), night.requiredEquipment());
        assertEquals(Set.of(NIGHT_PERMIT_OF_THE_REEF), night.requiredPermitsIn(REEF));
        assertEquals(base.estimatedDuration(), night.estimatedDuration());
        assertEquals(base.requiredSupplies(), night.requiredSupplies());
        assertEquals(base.requiredStaff(), night.requiredStaff());
    }

    @ParameterizedTest(name = "{0} risk by day is {1} at night")
    @CsvSource({"LOW, MEDIUM", "MEDIUM, HIGH", "HIGH, EXTREME", "EXTREME, EXTREME"})
    void raisesTheRiskOfTheBaseRulesOneLevelAndStaysAtTheMaximum(RiskLevel byDay, RiskLevel atNight) {
        assertEquals(atNight, new NightOperationRules(new FixedRules(byDay)).estimatedRisk());
    }

    @Test
    void asksForThePermitOfTheZoneTheActivityTakesPlaceIn() {
        final var night = new NightOperationRules(new SampleCollectionRules(3));

        assertEquals(Set.of(new Permit("night operations in zone z-2")), night.requiredPermitsIn(new Zone("z-2", "Bay", Set.of())));
    }

    @Test
    void keepsTheCertificationsAndThePermitsTheBaseRulesAlreadyAskFor() {
        final var night = new NightOperationRules(new DemandingRules());

        assertEquals(Set.of(FIRST_AID, NIGHT_OPERATIONS), night.requiredCertificationsForAllStaff());
        assertEquals(Set.of(RESEARCH_PERMIT, NIGHT_PERMIT_OF_THE_REEF), night.requiredPermitsIn(REEF));
    }

    @Test
    void rejectsMissingBaseRules() {
        assertThrows(NullPointerException.class, () -> new NightOperationRules(null));
    }

    /**
     * Stub with a fixed risk, so the risk of every level can be tested without depending on a concrete activity type.
     */
    private static class FixedRules implements ActivityRules {

        private final RiskLevel risk;

        FixedRules(RiskLevel risk) {
            this.risk = risk;
        }

        @Override
        public Duration estimatedDuration() {
            return Duration.ofHours(1);
        }

        @Override
        public RiskLevel estimatedRisk() {
            return risk;
        }

        @Override
        public List<ReusableRequirement> requiredEquipment() {
            return List.of();
        }

        @Override
        public List<DepletableRequirement> requiredSupplies() {
            return List.of();
        }

        @Override
        public List<StaffRequirement> requiredStaff() {
            return List.of(new StaffRequirement(1, Set.of()));
        }
    }

    private static class DemandingRules extends FixedRules {

        DemandingRules() {
            super(RiskLevel.LOW);
        }

        @Override
        public Set<Certification> requiredCertificationsForAllStaff() {
            return Set.of(FIRST_AID);
        }

        @Override
        public Set<Permit> requiredPermitsIn(Zone zone) {
            return Set.of(RESEARCH_PERMIT);
        }
    }
}
