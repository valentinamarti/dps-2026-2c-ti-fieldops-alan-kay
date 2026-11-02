package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.responses.AssignmentCandidates;
import ar.edu.itba.dps.fieldops.business.models.responses.InstrumentView;
import ar.edu.itba.dps.fieldops.business.models.responses.PersonView;
import ar.edu.itba.dps.fieldops.business.models.responses.SupplyView;
import ar.edu.itba.dps.fieldops.business.models.responses.VehicleView;
import ar.edu.itba.dps.fieldops.business.ports.in.FindAssignmentCandidatesPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.List;
import java.util.Objects;

public class FindAssignmentCandidates implements FindAssignmentCandidatesPort {

    private final ExpeditionRepository repository;
    private final ResourceCatalog catalog;

    public FindAssignmentCandidates(ExpeditionRepository repository, ResourceCatalog catalog) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public AssignmentCandidates candidatesFor(ExpeditionId expeditionId, ActivityId activityId) {
        final var item = Lookups.item(Lookups.expedition(repository, expeditionId), activityId);
        final var activity = item.getActivity();
        final var period = item.getScheduledPeriod();
        return new AssignmentCandidates(
                activity.requiredStaff().stream()
                        .map(requirement -> new AssignmentCandidates.ForStaff(requirement,
                                catalog.availableStaffFor(requirement, period).stream()
                                        .filter(activity::admits).map(PersonView::of).toList()))
                        .toList(),
                activity.requiredEquipment().stream()
                        .map(requirement -> equipmentFor(requirement, catalog.availableEquipmentFor(requirement, period)))
                        .toList(),
                activity.requiredSupplies().stream()
                        .map(requirement -> new AssignmentCandidates.ForSupplies(requirement,
                                ResourceKinds.only(ResourceKinds.SUPPLIES, catalog.suppliesCovering(requirement)).stream()
                                        .map(SupplyView::of).toList()))
                        .toList());
    }

    private AssignmentCandidates.ForEquipment equipmentFor(ReusableRequirement requirement, List<Equipment> available) {
        return new AssignmentCandidates.ForEquipment(requirement,
                ResourceKinds.only(ResourceKinds.VEHICLES, available).stream().map(VehicleView::of).toList(),
                ResourceKinds.only(ResourceKinds.INSTRUMENTS, available).stream().map(InstrumentView::of).toList());
    }
}
