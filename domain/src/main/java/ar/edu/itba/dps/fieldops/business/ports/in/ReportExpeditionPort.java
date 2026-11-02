package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.report.ExpeditionReport;

public interface ReportExpeditionPort {
    ExpeditionReport report(ExpeditionId id);
}
