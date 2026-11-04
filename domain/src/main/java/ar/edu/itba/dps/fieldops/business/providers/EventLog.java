package ar.edu.itba.dps.fieldops.business.providers;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.RecordedEventView;

import java.util.List;

public interface EventLog {
    List<RecordedEventView> ofExpedition(ExpeditionId expedition);
}
