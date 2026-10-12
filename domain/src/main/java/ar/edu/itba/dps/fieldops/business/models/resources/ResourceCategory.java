package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.util.Locale;

public record ResourceCategory(String name) {

    public ResourceCategory {
        name = DomainArguments.requireText(name, "name").strip().toLowerCase(Locale.ROOT);
    }
}