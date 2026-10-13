package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;

import java.util.List;

public record AssignmentCandidates(List<For<StaffRequirement, Person>> staff,
                                   List<For<ReusableRequirement, Equipment>> equipment,
                                   List<For<DepletableRequirement, DepletableResource>> supplies) {
    public record For<R, C>(R requirement, List<C> candidates) {
    }
}
