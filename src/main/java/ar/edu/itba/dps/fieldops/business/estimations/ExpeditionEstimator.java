package ar.edu.itba.dps.fieldops.business.estimations;

import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.estimation.ExpeditionEstimate;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ExpeditionEstimator {

    public ExpeditionEstimate estimate(Expedition expedition) {
        final var activities = expedition.getItinerary().getItems().stream().map(ItineraryItem::getActivity).toList();
        return new ExpeditionEstimate(totalDuration(activities), overallRisk(activities), estimatedConsumption(activities));
    }

    private Duration totalDuration(List<Activity> activities) {
        return activities.stream().map(Activity::estimatedDuration).reduce(Duration.ZERO, Duration::plus);
    }

    private RiskLevel overallRisk(List<Activity> activities) {
        return activities.stream()
                .map(Activity::estimatedRisk)
                .reduce(RiskLevel.LOW, (highest, risk) -> risk.isAbove(highest) ? risk : highest);
    }

    private Map<ResourceCategory, Quantity> estimatedConsumption(List<Activity> activities) {
        final var consumption = new LinkedHashMap<ResourceCategory, Quantity>();
        activities.stream()
                .flatMap(activity -> activity.requiredSupplies().stream())
                .forEach(requirement -> consumption.merge(requirement.category(), requirement.quantity(), Quantity::plus));
        return consumption;
    }
}
