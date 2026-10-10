package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;

import java.util.Objects;

public class ReplanExpedition {

    private final ExpeditionRepository repository;
    private final ExpeditionReplanner replanner;

    public ReplanExpedition(ExpeditionRepository repository, ExpeditionReplanner replanner) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.replanner = Objects.requireNonNull(replanner, "replanner is required");
    }

    public Expedition replan(Expedition original, ItineraryItem affectedItem, ReplanReason reason) {
        final var alternative = replanner.planAlternative(original, repository.nextId(), affectedItem, reason);
        repository.add(alternative);
        return alternative;
    }
}
