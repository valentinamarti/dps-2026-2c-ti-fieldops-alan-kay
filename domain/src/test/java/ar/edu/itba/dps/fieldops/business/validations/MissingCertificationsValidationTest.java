package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.DIVING;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.NIGHT_OPERATIONS;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.nightDiving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingCertificationsValidationTest {

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final MissingCertificationsValidation validation = new MissingCertificationsValidation();
    private final Zone reef = zone("z-1");
    private final Expedition expedition = expeditionIn(reef, person("p-1")).build(repository);
    private final ItineraryItem dive = expedition.schedule(diving("a-1", reef, hours(8, 18), 10), hours(9, 11));

    @Test
    void acceptsADiveWithTwoCertifiedDivers() {
        expedition.assignStaff(dive, person("p-2", DIVING));
        expedition.assignStaff(dive, person("p-3", DIVING));

        assertTrue(validation.validate(expedition).isEmpty());
    }

    @Test
    void flagsADiveWithoutEnoughCertifiedDivers() {
        expedition.assignStaff(dive, person("p-2", DIVING));
        expedition.assignStaff(dive, person("p-3"));

        final var expected = ValidationResult.critical(MissingCertificationsValidation.ID, ValidationSubject.of(new ActivityId("a-1")), "Diving a-1 needs 2 people certified in diving but only 1 qualify");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    private ItineraryItem nightDiveIn(Expedition night) {
        return night.schedule(nightDiving("a-2", reef, hours(0, 24), 10), hours(20, 22));
    }

    @Test
    void flagsEachPersonAssignedToANightActivityWithoutTheNightCertification() {
        final var night = expeditionIn(reef, person("p-1")).build(repository);
        final var nightDive = nightDiveIn(night);
        night.assignStaff(nightDive, person("p-2", DIVING));
        night.assignStaff(nightDive, person("p-3", DIVING));

        final var expected = List.of(
                ValidationResult.critical(MissingCertificationsValidation.ID, ValidationSubject.of(new ActivityId("a-2"), new ResourceId("p-2")), "Night diving a-2 requires everyone assigned to be certified in night operations, but p-2 is not"),
                ValidationResult.critical(MissingCertificationsValidation.ID, ValidationSubject.of(new ActivityId("a-2"), new ResourceId("p-3")), "Night diving a-2 requires everyone assigned to be certified in night operations, but p-3 is not"));
        assertEquals(expected, validation.validate(night));
    }

    @Test
    void flagsAnExtraPersonWithoutTheNightCertificationEvenIfTheHeadcountIsCovered() {
        final var night = expeditionIn(reef, person("p-1")).build(repository);
        final var nightDive = nightDiveIn(night);
        night.assignStaff(nightDive, person("p-2", DIVING, NIGHT_OPERATIONS));
        night.assignStaff(nightDive, person("p-3", DIVING, NIGHT_OPERATIONS));
        night.assignStaff(nightDive, person("p-4"));

        final var expected = ValidationResult.critical(MissingCertificationsValidation.ID, ValidationSubject.of(new ActivityId("a-2"), new ResourceId("p-4")), "Night diving a-2 requires everyone assigned to be certified in night operations, but p-4 is not");
        assertEquals(List.of(expected), validation.validate(night));
    }

    @Test
    void acceptsANightActivityWhoseStaffIsAllCertifiedForNightOperations() {
        final var night = expeditionIn(reef, person("p-1")).build(repository);
        final var nightDive = nightDiveIn(night);
        night.assignStaff(nightDive, person("p-2", DIVING, NIGHT_OPERATIONS));
        night.assignStaff(nightDive, person("p-3", DIVING, NIGHT_OPERATIONS));

        assertTrue(validation.validate(night).isEmpty());
    }
}
