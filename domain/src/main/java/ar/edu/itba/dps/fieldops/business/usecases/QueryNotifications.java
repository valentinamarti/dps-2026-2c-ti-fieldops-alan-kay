package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.notifications.Notification;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryNotificationsPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.NotificationRepository;

import java.util.List;
import java.util.Objects;

public class QueryNotifications implements QueryNotificationsPort {
    private final NotificationRepository notifications;
    private final ExpeditionRepository expeditions;

    public QueryNotifications(NotificationRepository notifications, ExpeditionRepository expeditions) {
        this.notifications = Objects.requireNonNull(notifications, "notifications are required");
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
    }

    @Override
    public List<Notification> ofExpedition(ExpeditionId expeditionId) {
        return notifications.ofExpedition(Lookups.expedition(expeditions, expeditionId).getId());
    }
}
