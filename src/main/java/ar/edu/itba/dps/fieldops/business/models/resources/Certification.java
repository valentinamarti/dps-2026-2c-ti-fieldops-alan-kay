package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

public record Certification(String name) {

    public Certification {
        name = DomainArguments.requireText(name, "name");
    }
}