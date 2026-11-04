package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.RecordedEventView;

import java.util.List;

public interface QueryExpeditionEventsPort {
    List<RecordedEventView> ofExpedition(ExpeditionId expeditionId);
}
