package ar.edu.itba.dps.fieldops.business.interfaces.itinerary;

import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface ItineraryNode {
    TimePeriod getScheduledPeriod();

    Duration estimatedDuration();

    RiskLevel estimatedRisk();

    List<DepletableRequirement> requiredSupplies();

    boolean isFinished();

    List<ItineraryItem> leaves();

    boolean runsInParallel(ItineraryItem first, ItineraryItem second);

    List<ItineraryItem> itemsAfter(ItineraryItem item);

    Optional<PlannedNode> planKeepingStructure(Function<ItineraryItem, Optional<PlannedNode>> itemPlans);

    <T> T accept(ItineraryNodeVisitor<T> visitor);
}
