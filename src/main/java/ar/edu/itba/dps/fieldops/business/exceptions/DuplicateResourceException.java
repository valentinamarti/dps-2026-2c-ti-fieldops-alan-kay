package ar.edu.itba.dps.fieldops.business.exceptions;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceId) {
        super("A resource with id %s is already registered".formatted(resourceId));
    }
}
