package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.util.List;

public interface QueryExpeditionsPort {
    Expedition get(ExpeditionId id);

    List<Expedition> list();

    boolean isStaleAlternative(Expedition expedition);
}
