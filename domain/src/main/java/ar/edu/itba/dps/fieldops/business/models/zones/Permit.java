package ar.edu.itba.dps.fieldops.business.models.zones;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

public record Permit(String name) {

    public Permit {
        name = DomainArguments.requireText(name, "name");
    }
}