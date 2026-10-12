package ar.edu.itba.dps.fieldops.business.exceptions;

public class DuplicateZoneException extends RuntimeException {

    public DuplicateZoneException(String zoneId) {
        super("A zone with id %s is already registered".formatted(zoneId));
    }
}
