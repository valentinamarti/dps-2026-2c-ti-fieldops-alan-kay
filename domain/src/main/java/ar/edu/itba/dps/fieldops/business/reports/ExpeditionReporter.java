package ar.edu.itba.dps.fieldops.business.reports;

import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.report.ExpeditionReport;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExpeditionReporter {

    private final ExpeditionEstimator estimator;

    public ExpeditionReporter(ExpeditionEstimator estimator) {
        this.estimator = Objects.requireNonNull(estimator, "estimator is required");
    }

    public ExpeditionReport reportOf(Expedition expedition) {
        final var items = expedition.getItinerary().getItems();
        return new ExpeditionReport(
                expedition.getName(),
                expedition.getStatus(),
                items.size(),
                (int) items.stream().filter(ItineraryItem::isFinished).count(),
                estimator.estimate(expedition),
                actualConsumption(items),
                incidents(items),
                expedition.getAcceptedWarnings());
    }

    /**
     * Only finished activities consumed anything, and only through the supply that covered each requirement.
     */
    private Map<ResourceCategory, Quantity> actualConsumption(List<ItineraryItem> items) {
        final var consumed = new LinkedHashMap<ResourceCategory, Quantity>();
        items.stream()
                .filter(ItineraryItem::isFinished)
                .forEach(item -> item.getActivity().requiredSupplies().forEach(requirement -> item.supplyFor(requirement)
                        .ifPresent(supply -> consumed.merge(requirement.category(), requirement.quantity(), Quantity::plus))));
        return consumed;
    }

    private List<Incident> incidents(List<ItineraryItem> items) {
        return items.stream()
                .flatMap(item -> item.getTracking().stream())
                .flatMap(tracking -> tracking.getIncidents().stream())
                .toList();
    }
}
