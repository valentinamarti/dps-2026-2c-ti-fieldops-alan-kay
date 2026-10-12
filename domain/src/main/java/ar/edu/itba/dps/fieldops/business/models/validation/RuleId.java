package ar.edu.itba.dps.fieldops.business.models.validation;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

public record RuleId(String value) {

    public RuleId {
        value = DomainArguments.requireText(value, "id");
    }

    @Override
    public String toString() {
        return value;
    }
}
