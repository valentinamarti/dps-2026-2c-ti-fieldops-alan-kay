package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.business.exceptions.ConcurrentUpdateException;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionSnapshot;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterIncidentPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterIncident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Sql("/clean-database.sql")
class TransactionalUseCasesTest {
    @Autowired
    private TransactionalUseCases transactional;
    @Autowired
    private ExpeditionRepository expeditions;
    @Autowired
    private ReplanProposalRepository proposals;
    @Autowired
    private ZoneRepository zones;
    @Autowired
    private ResourceCatalog catalog;

    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private ExpeditionId id;

    @BeforeEach
    void storeAnExpeditionInDraft() {
        zones.add(coast);
        catalog.addStaff(leader);
        final var expedition = expeditionIn(coast, leader).build(expeditions);
        expeditions.add(expedition);
        id = expedition.getId();
    }

    private Expedition stored() {
        return expeditions.findById(id).orElseThrow();
    }

    private void submitForReview() {
        final var expedition = stored();
        expedition.submitForReview();
        expeditions.save(expedition);
    }

    @Test
    void aFailureUndoesEverythingTheUseCaseSaved() {
        final Runnable failing = transactional.writing(Runnable.class, () -> {
            submitForReview();
            throw new IllegalArgumentException("the use case failed");
        });

        assertThrows(IllegalArgumentException.class, failing::run);

        assertEquals(ExpeditionStatus.DRAFT, stored().getStatus());
    }

    @Test
    void aFailingSubscriberUndoesTheIncidentThatPublishedTheEvent() {
        final Activity activity = sampling("a-1", coast, hours(0, 24));
        final var running = expeditionIn(coast, leader).build(expeditions);
        running.schedule(activity, hours(9, 11));
        catalog.addStaff(person("p-2"));
        running.submitForReview();
        final var approved = Expedition.restore(inExecution(running));
        expeditions.add(approved);
        final RegisterIncidentPort failing = transactional.writing(RegisterIncidentPort.class, new RegisterIncident(expeditions, event -> {
            throw new IllegalStateException("the subscriber failed");
        }, proposals));

        assertThrows(IllegalStateException.class, () -> failing.register(new RegisterIncidentCommand(approved.getId(), activity.getId(),
                IncidentReport.of("Storm", LocalDateTime.of(2026, 10, 1, 9, 0)))));

        assertTrue(expeditions.findById(approved.getId()).orElseThrow().getIncidents().isEmpty());
    }

    private ExpeditionSnapshot inExecution(Expedition expedition) {
        final var snapshot = expedition.snapshot();
        return new ExpeditionSnapshot(snapshot.id(), snapshot.name(), snapshot.period(), snapshot.objectives(),
                snapshot.zones(), snapshot.responsibles(), snapshot.restrictions(), snapshot.grantedPermits(), ExpeditionStatus.IN_EXECUTION,
                snapshot.acceptedWarnings(), snapshot.incidents(), snapshot.replacedPlan(), snapshot.replanReason(),
                snapshot.replacement(), snapshot.executionRevision(), snapshot.roots());
    }

    @Test
    void twoConflictingWritesLetOneWinAndTellTheOtherOne() {
        final Runnable conflicting = transactional.writing(Runnable.class, () -> {
            final var expedition = stored();
            expedition.submitForReview();
            runInAnotherTransaction();
            expeditions.save(expedition);
        });

        assertThrows(ConcurrentUpdateException.class, conflicting::run);

        assertEquals(ExpeditionStatus.IN_REVIEW, stored().getStatus());
    }

    private void runInAnotherTransaction() {
        final Runnable winner = transactional.writing(Runnable.class, this::submitForReview);
        final var thread = new Thread(winner);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }
}
