package ar.edu.itba.dps.fieldops.business.exceptions;

public class ConcurrentUpdateException extends RuntimeException {

    public ConcurrentUpdateException(Throwable cause) {
        super("Someone else changed this data at the same time: read it again and retry", cause);
    }
}
