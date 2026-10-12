package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Certifiable;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import lombok.Getter;

import java.util.Objects;
import java.util.Set;

public class Person extends ScheduledResource implements Certifiable {

    @Getter
    private final ResourceId id;
    @Getter
    private final String name;
    private final Certifications certifications;

    public Person(ResourceId id, String name, Set<Certification> certifications) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = DomainArguments.requireText(name, "name");
        this.certifications = new Certifications(certifications);
    }

    @Override
    public boolean hasCertification(Certification certification) {
        return certifications.includes(certification);
    }
}
