package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.notifications.Notification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.providers.NotificationRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Transactional
public class JpaNotificationRepository implements NotificationRepository {
    private final NotificationJpaRepository notifications;

    public JpaNotificationRepository(NotificationJpaRepository notifications) {
        this.notifications = Objects.requireNonNull(notifications, "notifications are required");
    }

    @Override
    public void add(Notification notification) {
        Objects.requireNonNull(notification, "notification is required");
        notifications.save(toEntity(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> ofExpedition(ExpeditionId expedition) {
        Objects.requireNonNull(expedition, "expedition is required");
        return notifications.findByExpeditionIdOrderBySequenceAsc(expedition.value()).stream()
                .map(JpaNotificationRepository::toNotification)
                .toList();
    }

    private static NotificationEntity toEntity(Notification notification) {
        final var entity = new NotificationEntity();
        entity.setEventId(notification.event().value());
        entity.setExpeditionId(notification.expedition().value());
        entity.setResponsibleId(notification.responsible().value());
        entity.setText(notification.text());
        entity.setOccurredAt(notification.occurredAt());
        return entity;
    }

    private static Notification toNotification(NotificationEntity entity) {
        return new Notification(new EventId(entity.getEventId()), new ExpeditionId(entity.getExpeditionId()),
                new ResourceId(entity.getResponsibleId()), entity.getText(), entity.getOccurredAt());
    }
}
