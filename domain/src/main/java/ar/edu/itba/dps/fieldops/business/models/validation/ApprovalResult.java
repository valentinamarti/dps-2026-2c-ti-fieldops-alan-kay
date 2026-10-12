package ar.edu.itba.dps.fieldops.business.models.validation;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.util.List;

public record ApprovalResult(List<ValidationResult> results) {

    public ApprovalResult {
        results = DomainArguments.requireList(results, "results");
    }

    public List<ValidationResult> criticals() {
        return withSeverity(Severity.CRITICAL);
    }

    public List<ValidationResult> warnings() {
        return withSeverity(Severity.WARNING);
    }

    public boolean canBeApproved() {
        return criticals().isEmpty();
    }

    private List<ValidationResult> withSeverity(Severity severity) {
        return results.stream().filter(result -> result.severity() == severity).toList();
    }
}