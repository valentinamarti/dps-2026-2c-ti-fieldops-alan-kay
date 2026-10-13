package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Certifiable;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import lombok.Getter;

import java.util.Objects;
import java.util.Set;

public class Instrument extends ScheduledResource implements Equipment, Certifiable {

    @Getter
    private final ResourceId id;
    @Getter
    private final String name;
    @Getter
    private final ResourceCategory category;
    @Getter
    private final Certifications certifications;

    public Instrument(ResourceId id, String name, ResourceCategory category, Set<Certification> certifications) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = DomainArguments.requireText(name, "name");
        this.category = Objects.requireNonNull(category, "category is required");
        this.certifications = new Certifications(certifications);
    }

    @Override
    public boolean belongsTo(ResourceCategory category) {
        return this.category.equals(category);
    }

    @Override
    public boolean hasCertification(Certification certification) {
        return certifications.includes(certification);
    }
}
