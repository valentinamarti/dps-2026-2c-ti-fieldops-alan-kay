package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.ExpeditionReportView;

public interface ReportExpeditionPort {
    ExpeditionReportView report(ExpeditionId id);
}
