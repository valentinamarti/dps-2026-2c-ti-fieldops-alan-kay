package ar.edu.itba.dps.fieldops.business.models.replanning;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

public record ReplanProposalId(String value) {

    public ReplanProposalId {
        value = DomainArguments.requireText(value, "id");
    }

    @Override
    public String toString() {
        return value;
    }
}
