package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.NotificationResponse;
import ar.edu.itba.dps.fieldops.business.models.notifications.Notification;

public final class NotificationMapper {
    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.event().value(), notification.responsible().value(),
                notification.text(), notification.occurredAt());
    }
}
