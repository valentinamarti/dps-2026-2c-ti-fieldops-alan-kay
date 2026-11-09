package ar.edu.itba.dps.fieldops.business.providers;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.report.ProgressEntry;

import java.util.List;

public interface ProgressHistory {
    void add(ProgressEntry entry);

    List<ProgressEntry> ofExpedition(ExpeditionId expedition);
}
