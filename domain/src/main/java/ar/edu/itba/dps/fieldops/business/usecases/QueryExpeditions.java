package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.ExpeditionView;
import ar.edu.itba.dps.fieldops.business.models.responses.ItineraryView;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryExpeditionsPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.List;
import java.util.Objects;

public class QueryExpeditions implements QueryExpeditionsPort {

    private final ExpeditionRepository repository;

    public QueryExpeditions(ExpeditionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    @Override
    public ExpeditionView get(ExpeditionId id) {
        return viewOf(Lookups.expedition(repository, id));
    }

    @Override
    public List<ExpeditionView> list() {
        return repository.findAll().stream().map(this::viewOf).toList();
    }

    @Override
    public ItineraryView itinerary(ExpeditionId id) {
        return ItineraryView.of(Lookups.expedition(repository, id));
    }

    private ExpeditionView viewOf(Expedition expedition) {
        return ExpeditionView.of(expedition, isStaleAlternative(expedition));
    }

    private boolean isStaleAlternative(Expedition expedition) {
        return expedition.getReplacedExpedition()
                .flatMap(repository::findById)
                .filter(original -> !expedition.isBuiltOnCurrentPlanOf(original))
                .isPresent();
    }
}
