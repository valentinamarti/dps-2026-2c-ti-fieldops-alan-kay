package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

public record Certification(String name) {

    // TODO: review when the teacher replies to our questions - maybe new fields? maybe certification type enum?
    public Certification {
        name = DomainArguments.requireText(name, "name");
    }
}