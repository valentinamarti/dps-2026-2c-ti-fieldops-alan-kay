package ar.edu.itba.dps.fieldops.business.exceptions;

import java.util.List;

public class ActivitiesInProgressException extends RuntimeException {

    public ActivitiesInProgressException(List<String> activityNames) {
        super("An expedition cannot finish while these activities are still in progress: %s"
                .formatted(String.join(", ", activityNames)));
    }
}
