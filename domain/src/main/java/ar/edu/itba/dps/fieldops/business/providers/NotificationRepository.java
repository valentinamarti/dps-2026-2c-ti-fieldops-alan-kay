package ar.edu.itba.dps.fieldops.business.providers;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.notifications.Notification;

import java.util.List;

public interface NotificationRepository {
    void add(Notification notification);

    List<Notification> ofExpedition(ExpeditionId expedition);
}
