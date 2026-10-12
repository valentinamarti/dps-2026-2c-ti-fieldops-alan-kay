package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanOptionGenerator;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ResourceAssignment;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanOption;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * When the incident names a resource, offers one option per catalog candidate that covers the same requirement,
 * is free in the period of the activity and is admitted by it, in catalog order and up to {@link #MAX_CANDIDATES}
 * (D46).
 */
public class ResourceReplacementOptions implements ReplanOptionGenerator {

    public static final int MAX_CANDIDATES = 3;

    private final ResourceCatalog catalog;

    public ResourceReplacementOptions(ResourceCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public List<ReplanOption> optionsFor(Expedition expedition, ItineraryItem affected, Incident incident) {
        return incident.resource().stream()
                .flatMap(withdrawn -> candidatesFor(affected, withdrawn)
                        .limit(MAX_CANDIDATES)
                        .map(candidate -> new ReplanOption(new ResourceReplaced(withdrawn, candidate),
                                "Replace %s with %s in %s".formatted(withdrawn, candidate, affected.getActivity().getName()))))
                .toList();
    }

    private Stream<ResourceAssignment> candidatesFor(ItineraryItem item, ResourceId withdrawn) {
        return Stream.of(staffCandidates(item, withdrawn), equipmentCandidates(item, withdrawn), supplyCandidates(item, withdrawn))
                .flatMap(candidates -> candidates)
                .filter(candidate -> !isAssignedTo(item, candidate))
                .distinct();
    }

    private Stream<ResourceAssignment> staffCandidates(ItineraryItem item, ResourceId withdrawn) {
        final var activity = item.getActivity();
        return item.getAssignedStaff().stream()
                .filter(person -> person.getId().equals(withdrawn))
                .flatMap(person -> activity.requiredStaff().stream().filter(requirement -> requirement.qualifies(person)))
                .flatMap(requirement -> catalog.availableStaffFor(requirement, item.getScheduledPeriod()).stream())
                .filter(activity::admits)
                .map(ResourceAssignment::of);
    }

    private Stream<ResourceAssignment> equipmentCandidates(ItineraryItem item, ResourceId withdrawn) {
        return item.getAssignedEquipment().stream()
                .filter(equipment -> equipment.getId().equals(withdrawn))
                .flatMap(equipment -> item.getActivity().requiredEquipment().stream().filter(requirement -> requirement.accepts(equipment)))
                .flatMap(requirement -> catalog.availableEquipmentFor(requirement, item.getScheduledPeriod()).stream())
                .map(ResourceAssignment::of);
    }

    private Stream<ResourceAssignment> supplyCandidates(ItineraryItem item, ResourceId withdrawn) {
        return item.getAssignedSupplies().stream()
                .filter(supply -> supply.getId().equals(withdrawn))
                .flatMap(supply -> item.getActivity().requiredSupplies().stream().filter(requirement -> requirement.accepts(supply)))
                .flatMap(requirement -> catalog.suppliesCovering(requirement).stream())
                .map(ResourceAssignment::of);
    }

    private boolean isAssignedTo(ItineraryItem item, ResourceAssignment candidate) {
        return Stream.concat(item.getAssignedReusableResources().stream(), item.getAssignedSupplies().stream())
                .anyMatch(assigned -> assigned.equals(candidate.resource()));
    }
}
