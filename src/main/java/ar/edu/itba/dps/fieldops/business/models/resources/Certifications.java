package ar.edu.itba.dps.fieldops.business.models.resources;

import java.util.Set;

public record Certifications(Set<Certification> values) {

    public Certifications {
        values = Set.copyOf(values);
    }

    public boolean includes(Certification certification) {
        return values.contains(certification);
    }
}
