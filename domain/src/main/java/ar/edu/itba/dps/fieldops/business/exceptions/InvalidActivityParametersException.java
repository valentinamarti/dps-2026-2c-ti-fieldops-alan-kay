package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityTypeName;

public class InvalidActivityParametersException extends RuntimeException {

    public InvalidActivityParametersException(ActivityTypeName type, String reason) {
        super("An activity of type %s cannot be created: %s".formatted(type, reason));
    }
}
