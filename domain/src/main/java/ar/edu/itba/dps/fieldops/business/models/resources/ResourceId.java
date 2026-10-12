package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

public record ResourceId(String value) {

    public ResourceId {
        value = DomainArguments.requireText(value, "id");
    }

    @Override
    public String toString() {
        return value;
    }
}
