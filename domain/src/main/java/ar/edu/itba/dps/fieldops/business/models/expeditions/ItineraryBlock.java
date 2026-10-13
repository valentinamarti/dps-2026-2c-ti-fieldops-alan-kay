package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNodeVisitor;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class ItineraryBlock implements ItineraryNode {
    private final List<ItineraryNode> children;

    ItineraryBlock(List<ItineraryNode> children) {
        this.children = DomainArguments.requireList(children, "children");
        if (this.children.isEmpty()) {
            throw new InvalidScheduleException("a block needs at least one activity or block inside");
        }
    }

    @Override
    public TimePeriod getScheduledPeriod() {
        return new TimePeriod(
                children.stream().map(child -> child.getScheduledPeriod().start()).min(LocalDateTime::compareTo).orElseThrow(),
                children.stream().map(child -> child.getScheduledPeriod().end()).max(LocalDateTime::compareTo).orElseThrow());
    }

    @Override
    public RiskLevel estimatedRisk() {
        return children.stream().map(ItineraryNode::estimatedRisk).max(Comparator.naturalOrder()).orElseThrow();
    }

    @Override
    public List<DepletableRequirement> requiredSupplies() {
        return children.stream().flatMap(child -> child.requiredSupplies().stream()).toList();
    }

    @Override
    public boolean isFinished() {
        return children.stream().allMatch(ItineraryNode::isFinished);
    }

    @Override
    public List<ItineraryItem> leaves() {
        return children.stream().flatMap(child -> child.leaves().stream()).toList();
    }

    @Override
    public boolean runsInParallel(ItineraryItem first, ItineraryItem second) {
        return children.stream().anyMatch(child -> child.runsInParallel(first, second));
    }

    @Override
    public List<ItineraryItem> itemsAfter(ItineraryItem item) {
        return children.stream().flatMap(child -> child.itemsAfter(item).stream()).toList();
    }

    @Override
    public Optional<PlannedNode> planKeepingStructure(Function<ItineraryItem, Optional<PlannedNode>> itemPlans) {
        final var plans = children.stream().flatMap(child -> child.planKeepingStructure(itemPlans).stream()).toList();
        return plans.isEmpty() ? Optional.empty() : Optional.of(planOf(plans));
    }

    abstract PlannedNode planOf(List<PlannedNode> children);

    List<ItineraryNode> children() {
        return children;
    }

    <T> List<T> acceptedChildren(ItineraryNodeVisitor<T> visitor) {
        return children.stream().map(child -> child.accept(visitor)).toList();
    }
}
