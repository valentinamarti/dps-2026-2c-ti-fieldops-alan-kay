package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.WarningAcceptance;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.util.List;

public interface ApproveExpeditionPort {
    void approve(ExpeditionId id, List<WarningAcceptance> acceptances);
}
