package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.util.Set;

public record Certifications(Set<Certification> values) {

    public Certifications {
        values = DomainArguments.requireSet(values, "certifications");
    }

    public boolean includes(Certification certification) {
        return values.contains(certification);
    }
}