package ar.edu.itba.dps.fieldops.business.exceptions;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String kind, Object id) {
        super("There is no %s with id %s".formatted(kind, id));
    }
}
