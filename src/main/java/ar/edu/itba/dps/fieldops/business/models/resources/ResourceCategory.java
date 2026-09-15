package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

public record ResourceCategory(String name) {

    public ResourceCategory {
        name = DomainArguments.requireText(name, "name");
    }
}