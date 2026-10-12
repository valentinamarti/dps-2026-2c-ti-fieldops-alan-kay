package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.Objects;

public class FindAssignmentCandidates {

    private final ExpeditionRepository repository;
    private final ResourceCatalog catalog;

    public FindAssignmentCandidates(ExpeditionRepository repository, ResourceCatalog catalog) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    public AssignmentCandidates candidatesFor(ExpeditionId expeditionId, ActivityId activityId) {
        final var item = Lookups.item(Lookups.expedition(repository, expeditionId), activityId);
        final var activity = item.getActivity();
        final var period = item.getScheduledPeriod();
        return new AssignmentCandidates(
                activity.requiredStaff().stream()
                        .map(requirement -> new AssignmentCandidates.For<>(requirement, catalog.availableStaffFor(requirement, period)))
                        .toList(),
                activity.requiredEquipment().stream()
                        .map(requirement -> new AssignmentCandidates.For<>(requirement, catalog.availableEquipmentFor(requirement, period)))
                        .toList(),
                activity.requiredSupplies().stream()
                        .map(requirement -> new AssignmentCandidates.For<>(requirement, catalog.suppliesCovering(requirement)))
                        .toList());
    }
}
