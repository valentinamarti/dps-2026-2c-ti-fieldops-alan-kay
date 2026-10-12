package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.nightDiving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.nightPermit;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingPermitsValidationTest {

    private static final Permit MARINE_RESERVE = new Permit("Marine reserve access");
    private static final Permit NATIONAL_PARK = new Permit("National park access");

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final MissingPermitsValidation validation = new MissingPermitsValidation();
    private final Zone reserve = new Zone("z-1", "Marine reserve", Set.of(MARINE_RESERVE, NATIONAL_PARK));

    @Test
    void flagsEachPermitAZoneRequiresThatWasNotGranted() {
        final var expedition = expeditionIn(reserve, person("p-1")).addPermit(NATIONAL_PARK).build(repository);

        final var expected = ValidationResult.critical(MissingPermitsValidation.ID, ValidationSubject.expedition(), "Marine reserve requires the permit Marine reserve access, which was not granted");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void acceptsAnExpeditionWithEveryPermitItsZonesRequire() {
        final var expedition = expeditionIn(reserve, person("p-1")).addPermit(NATIONAL_PARK).addPermit(MARINE_RESERVE).build(repository);

        assertTrue(validation.validate(expedition).isEmpty());
    }

    @Test
    void flagsANightActivityWhoseZonePermitWasNotGranted() {
        final var reef = zone("z-2");
        final var expedition = expeditionIn(reef, person("p-1")).build(repository);
        expedition.schedule(nightDiving("a-1", reef, hours(0, 24), 10), hours(20, 22));

        final var expected = ValidationResult.critical(MissingPermitsValidation.ID, ValidationSubject.of(new ActivityId("a-1")), "Night diving a-1 requires the permit night operations in zone z-2, which was not granted");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void theNightPermitOfAnotherZoneDoesNotCoverTheActivity() {
        final var reef = zone("z-2");
        final var bay = zone("z-3");
        final var expedition = expeditionIn(reef, person("p-1")).addZone(bay).addPermit(nightPermit(bay)).build(repository);
        expedition.schedule(nightDiving("a-1", reef, hours(0, 24), 10), hours(20, 22));

        final var expected = ValidationResult.critical(MissingPermitsValidation.ID, ValidationSubject.of(new ActivityId("a-1")), "Night diving a-1 requires the permit night operations in zone z-2, which was not granted");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void acceptsANightActivityWhoseZonePermitWasGranted() {
        final var reef = zone("z-2");
        final var expedition = expeditionIn(reef, person("p-1")).addPermit(nightPermit(reef)).build(repository);
        expedition.schedule(nightDiving("a-1", reef, hours(0, 24), 10), hours(20, 22));

        assertTrue(validation.validate(expedition).isEmpty());
    }

    @Test
    void aDayActivityNeedsNoSpecialPermitOfItsZone() {
        final var reef = zone("z-2");
        final var expedition = expeditionIn(reef, person("p-1")).build(repository);
        expedition.schedule(diving("a-1", reef, hours(0, 24), 10), hours(9, 11));

        assertTrue(validation.validate(expedition).isEmpty());
    }
}