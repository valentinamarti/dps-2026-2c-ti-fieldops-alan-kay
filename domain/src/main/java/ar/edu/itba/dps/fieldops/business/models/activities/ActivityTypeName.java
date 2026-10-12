package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.util.Locale;

public record ActivityTypeName(String value) {

    public ActivityTypeName {
        value = DomainArguments.requireText(value, "name").strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
