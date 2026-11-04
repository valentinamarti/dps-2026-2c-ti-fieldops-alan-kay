package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.RecordedEventView;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryExpeditionEventsPort;
import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.List;
import java.util.Objects;

public class QueryExpeditionEvents implements QueryExpeditionEventsPort {
    private final EventLog events;
    private final ExpeditionRepository expeditions;

    public QueryExpeditionEvents(EventLog events, ExpeditionRepository expeditions) {
        this.events = Objects.requireNonNull(events, "events are required");
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
    }

    @Override
    public List<RecordedEventView> ofExpedition(ExpeditionId expeditionId) {
        return events.ofExpedition(Lookups.expedition(expeditions, expeditionId).getId());
    }
}
