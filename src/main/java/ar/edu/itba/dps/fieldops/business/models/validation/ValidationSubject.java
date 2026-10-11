package ar.edu.itba.dps.fieldops.business.models.validation;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.util.Objects;
import java.util.Optional;

public record ValidationSubject(Optional<ActivityId> activity, Optional<ResourceId> resource) {

    public ValidationSubject {
        Objects.requireNonNull(activity, "activity is required");
        Objects.requireNonNull(resource, "resource is required");
    }

    public static ValidationSubject expedition() {
        return new ValidationSubject(Optional.empty(), Optional.empty());
    }

    public static ValidationSubject of(ActivityId activity) {
        return new ValidationSubject(Optional.of(activity), Optional.empty());
    }

    public static ValidationSubject of(ResourceId resource) {
        return new ValidationSubject(Optional.empty(), Optional.of(resource));
    }

    public static ValidationSubject of(ActivityId activity, ResourceId resource) {
        return new ValidationSubject(Optional.of(activity), Optional.of(resource));
    }
}
