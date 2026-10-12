package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Certifiable;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;

import java.util.Set;


public record StaffRequirement(int headcount, Set<Certification> certifications) {

    public StaffRequirement {
        if (headcount <= 0) {
            throw new IllegalArgumentException("headcount must be positive");
        }
        certifications = DomainArguments.requireSet(certifications, "certifications");
    }

    public boolean qualifies(Certifiable candidate) {
        return certifications.stream().allMatch(candidate::hasCertification);
    }
}
