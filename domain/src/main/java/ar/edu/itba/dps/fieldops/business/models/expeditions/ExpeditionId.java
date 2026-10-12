package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

public record ExpeditionId(String value) {

    public ExpeditionId {
        value = DomainArguments.requireText(value, "id");
    }

    @Override
    public String toString() {
        return value;
    }
}
