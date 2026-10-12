package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransitionExpeditionTest {

    private final ExpeditionRepository repository = mock(ExpeditionRepository.class);
    private final TransitionExpedition transitions = new TransitionExpedition(repository);
    private final Expedition expedition = expeditionIn(zone("z-1"), person("p-1")).build(new SequentialExpeditionRepository());

    private static Stream<Arguments> transitionsThatAreAllowed() {
        return Stream.of(
                step("submitForReview", ExpeditionStatus.DRAFT, TransitionExpedition::submitForReview, ExpeditionStatus.IN_REVIEW),
                step("returnToDraft", ExpeditionStatus.IN_REVIEW, TransitionExpedition::returnToDraft, ExpeditionStatus.DRAFT),
                step("start", ExpeditionStatus.APPROVED, TransitionExpedition::start, ExpeditionStatus.IN_EXECUTION),
                step("suspend", ExpeditionStatus.IN_EXECUTION, TransitionExpedition::suspend, ExpeditionStatus.SUSPENDED),
                step("resume", ExpeditionStatus.SUSPENDED, TransitionExpedition::resume, ExpeditionStatus.IN_EXECUTION));
    }

    private static Stream<Arguments> transitionsThatAreNotAllowedFromDraft() {
        return Stream.of(
                Arguments.of("returnToDraft", (BiConsumer<TransitionExpedition, ExpeditionId>) TransitionExpedition::returnToDraft),
                Arguments.of("start", (BiConsumer<TransitionExpedition, ExpeditionId>) TransitionExpedition::start),
                Arguments.of("suspend", (BiConsumer<TransitionExpedition, ExpeditionId>) TransitionExpedition::suspend),
                Arguments.of("resume", (BiConsumer<TransitionExpedition, ExpeditionId>) TransitionExpedition::resume));
    }

    private static Arguments step(String name, ExpeditionStatus from, BiConsumer<TransitionExpedition, ExpeditionId> operation,
                                  ExpeditionStatus to) {
        return Arguments.of(name, from, operation, to);
    }

    private void givenAnExpeditionIn(ExpeditionStatus status) {
        if (status == ExpeditionStatus.IN_REVIEW || status == ExpeditionStatus.APPROVED
                || status == ExpeditionStatus.IN_EXECUTION || status == ExpeditionStatus.SUSPENDED) {
            expedition.submitForReview();
        }
        if (status == ExpeditionStatus.APPROVED || status == ExpeditionStatus.IN_EXECUTION || status == ExpeditionStatus.SUSPENDED) {
            expedition.approve(List.of(), List.of());
        }
        if (status == ExpeditionStatus.IN_EXECUTION || status == ExpeditionStatus.SUSPENDED) {
            expedition.start();
        }
        if (status == ExpeditionStatus.SUSPENDED) {
            expedition.suspend();
        }
        when(repository.findById(expedition.getId())).thenReturn(Optional.of(expedition));
    }

    @ParameterizedTest(name = "{0} takes {1} to {3}")
    @MethodSource("transitionsThatAreAllowed")
    void movesTheExpeditionToTheNextStatus(String name, ExpeditionStatus from, BiConsumer<TransitionExpedition, ExpeditionId> operation,
                                           ExpeditionStatus to) {
        givenAnExpeditionIn(from);

        operation.accept(transitions, expedition.getId());

        assertEquals(to, expedition.getStatus());
    }

    @ParameterizedTest(name = "{0} is refused from DRAFT")
    @MethodSource("transitionsThatAreNotAllowedFromDraft")
    void refusesATransitionTheStatusTableDoesNotAllow(String name, BiConsumer<TransitionExpedition, ExpeditionId> operation) {
        givenAnExpeditionIn(ExpeditionStatus.DRAFT);

        assertThrows(InvalidStatusTransitionException.class, () -> operation.accept(transitions, expedition.getId()));
        assertEquals(ExpeditionStatus.DRAFT, expedition.getStatus());
    }

    @Test
    void failsWhenTheExpeditionDoesNotExist() {
        when(repository.findById(new ExpeditionId("e-404"))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> transitions.submitForReview(new ExpeditionId("e-404")));
    }
}
