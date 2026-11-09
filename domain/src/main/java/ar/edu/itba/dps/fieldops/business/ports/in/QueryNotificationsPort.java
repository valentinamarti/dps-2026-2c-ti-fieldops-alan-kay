package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.notifications.Notification;

import java.util.List;

public interface QueryNotificationsPort {
    List<Notification> ofExpedition(ExpeditionId expeditionId);
}
