package ar.edu.itba.dps.fieldops.business.models.events;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

public record ConsumerName(String value) {

    public ConsumerName {
        value = DomainArguments.requireText(value, "name");
    }

    @Override
    public String toString() {
        return value;
    }
}
