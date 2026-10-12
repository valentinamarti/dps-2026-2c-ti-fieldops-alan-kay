package ar.edu.itba.dps.fieldops.business.estimations;

import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.estimation.ExpeditionEstimate;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ExpeditionEstimator {

    public ExpeditionEstimate estimate(Expedition expedition) {
        final var nodes = expedition.getItinerary().getNodes();
        return new ExpeditionEstimate(totalDuration(nodes), overallRisk(nodes), estimatedConsumption(nodes));
    }

    private Duration totalDuration(List<ItineraryNode> nodes) {
        return nodes.stream().map(ItineraryNode::estimatedDuration).reduce(Duration.ZERO, Duration::plus);
    }

    private RiskLevel overallRisk(List<ItineraryNode> nodes) {
        return nodes.stream()
                .map(ItineraryNode::estimatedRisk)
                .reduce(RiskLevel.LOW, (highest, risk) -> risk.isAbove(highest) ? risk : highest);
    }

    private Map<ResourceCategory, Quantity> estimatedConsumption(List<ItineraryNode> nodes) {
        final var consumption = new LinkedHashMap<ResourceCategory, Quantity>();
        nodes.stream()
                .flatMap(node -> node.requiredSupplies().stream())
                .forEach(requirement -> consumption.merge(requirement.category(), requirement.quantity(), Quantity::plus));
        return consumption;
    }
}
