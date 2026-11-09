package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.events.AsyncEventSubscriber;
import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.notifications.Notification;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.NotificationRepository;

import java.util.Objects;

public class NotifyResponsibles implements AsyncEventSubscriber<DomainEvent> {
    private static final ConsumerName NAME = new ConsumerName("notify-responsibles");

    private final ExpeditionRepository expeditions;
    private final NotificationRepository notifications;

    public NotifyResponsibles(ExpeditionRepository expeditions, NotificationRepository notifications) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
        this.notifications = Objects.requireNonNull(notifications, "notifications are required");
    }

    @Override
    public Class<DomainEvent> eventType() {
        return DomainEvent.class;
    }

    @Override
    public ConsumerName consumerName() {
        return NAME;
    }

    @Override
    public void on(DomainEvent event) {
        Lookups.expedition(expeditions, event.expeditionId()).getResponsibles().stream()
                .map(Person::getId)
                .map(responsible -> Notification.of(event, responsible))
                .forEach(notifications::add);
    }
}
