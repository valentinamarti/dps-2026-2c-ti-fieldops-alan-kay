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

    /**
     * True when this node declares that the two items may run at the same time, whatever their scheduled periods are.
     */
    boolean runsInParallel(ItineraryItem first, ItineraryItem second);

    /**
     * The items this node runs after the given one because they come later in a sequential block, at any depth.
     * Empty when the item is not inside this node.
     */
    List<ItineraryItem> itemsAfter(ItineraryItem item);

    /**
     * The plan of a node with this same shape, asking for the plan of each item. Items without a plan are left out,
     * and so is a node left with nothing inside.
     */
    Optional<PlannedNode> planKeepingStructure(Function<ItineraryItem, Optional<PlannedNode>> itemPlans);
}
