package ar.edu.itba.dps.fieldops.business.providers;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

@FunctionalInterface
public interface ExpeditionIdProvider {
    ExpeditionId nextId();
}
