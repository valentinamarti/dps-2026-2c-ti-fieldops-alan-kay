package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityTypeName;

import java.util.Collection;
import java.util.stream.Collectors;

public class UnknownActivityTypeException extends RuntimeException {

    public UnknownActivityTypeException(ActivityTypeName name, Collection<ActivityTypeName> available) {
        super("There is no activity type named %s. The available types are: %s".formatted(
                name, available.stream().map(ActivityTypeName::value).collect(Collectors.joining(", "))));
    }
}
