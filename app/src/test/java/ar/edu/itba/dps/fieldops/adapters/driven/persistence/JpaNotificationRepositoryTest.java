package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.notifications.Notification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.providers.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class JpaNotificationRepositoryTest {
    private static final ExpeditionId PATAGONIA = new ExpeditionId("e-1");
    private static final ExpeditionId NIGHT_SURVEY = new ExpeditionId("e-2");
    private static final ResourceId ADA = new ResourceId("p-1");
    private static final ResourceId LEE = new ResourceId("p-2");
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 2, 1, 10, 0);

    @Autowired
    private NotificationRepository notifications;
    @Autowired
    private DatabaseCleaner cleaner;

    private static Notification notification(String event, ExpeditionId expedition, ResourceId responsible, LocalDateTime at) {
        return new Notification(new EventId(event), expedition, responsible, "The expedition was approved", at);
    }

    @BeforeEach
    void startEmpty() {
        cleaner.clean();
    }

    @Test
    void givesBackWhatItSaved() {
        final var saved = notification("ev-1", PATAGONIA, ADA, TEN);

        notifications.add(saved);

        assertEquals(List.of(saved), notifications.ofExpedition(PATAGONIA));
    }

    @Test
    void keepsTheOrderTheyArrivedInEvenIfTheFactsHappenedInAnotherOrder() {
        final var later = notification("ev-1", PATAGONIA, ADA, TEN.plusHours(1));
        final var earlier = notification("ev-2", PATAGONIA, ADA, TEN);

        notifications.add(later);
        notifications.add(earlier);

        assertEquals(List.of(later, earlier), notifications.ofExpedition(PATAGONIA));
    }

    @Test
    void leavesOutTheNotificationsOfAnotherExpedition() {
        final var ofPatagonia = notification("ev-1", PATAGONIA, ADA, TEN);
        notifications.add(ofPatagonia);
        notifications.add(notification("ev-2", NIGHT_SURVEY, ADA, TEN));

        assertEquals(List.of(ofPatagonia), notifications.ofExpedition(PATAGONIA));
    }

    @Test
    void anEventNotifiesEachResponsibleOnce() {
        notifications.add(notification("ev-1", PATAGONIA, ADA, TEN));
        notifications.add(notification("ev-1", PATAGONIA, LEE, TEN));

        assertEquals(2, notifications.ofExpedition(PATAGONIA).size());
    }

    @Test
    void rejectsASecondNotificationOfTheSameEventToTheSameResponsible() {
        notifications.add(notification("ev-1", PATAGONIA, ADA, TEN));

        assertThrows(DataIntegrityViolationException.class, () -> notifications.add(notification("ev-1", PATAGONIA, ADA, TEN)));
        assertEquals(1, notifications.ofExpedition(PATAGONIA).size());
    }

    @Test
    void keepsATextLongerThanAnIncidentDescription() {
        final var longText = new Notification(new EventId("ev-1"), PATAGONIA, ADA, "x".repeat(1500), TEN);

        notifications.add(longText);

        assertEquals(List.of(longText), notifications.ofExpedition(PATAGONIA));
    }
}
