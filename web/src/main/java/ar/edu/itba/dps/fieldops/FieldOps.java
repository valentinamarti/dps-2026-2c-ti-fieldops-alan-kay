package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import ar.edu.itba.dps.fieldops.business.usecases.ApproveExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.CreateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.FinishExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.ReplanExpedition;

public record FieldOps(CreateExpedition createExpedition,
                       ApproveExpedition approveExpedition,
                       ReplanExpedition replanExpedition,
                       FinishExpedition finishExpedition,
                       ExpeditionReporter reporter) {
}
