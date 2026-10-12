package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

public record ActivityId(String value) {

    public ActivityId {
        value = DomainArguments.requireText(value, "id");
    }

    @Override
    public String toString() {
        return value;
    }
}
