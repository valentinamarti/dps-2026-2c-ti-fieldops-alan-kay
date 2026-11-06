package ar.edu.itba.dps.fieldops.adapters.driven;

public class UnknownEventTypeException extends RuntimeException {

    UnknownEventTypeException(String type) {
        super("The recorded event type %s is not a domain event".formatted(type));
    }

    UnknownEventTypeException(String type, Throwable cause) {
        super("There is no event type named %s. A recorded event cannot be read if its class was renamed or moved"
                .formatted(type), cause);
    }
}
