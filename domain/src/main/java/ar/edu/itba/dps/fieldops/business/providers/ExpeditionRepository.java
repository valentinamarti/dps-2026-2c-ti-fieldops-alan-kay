package ar.edu.itba.dps.fieldops.business.providers;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.util.Optional;

public interface ExpeditionRepository {

    ExpeditionId nextId();

    void add(Expedition expedition);

    Optional<Expedition> findById(ExpeditionId id);
}
