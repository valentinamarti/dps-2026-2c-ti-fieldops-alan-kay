package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(ResourceId resourceId) {
        super("A resource with id %s is already registered".formatted(resourceId));
    }
}
