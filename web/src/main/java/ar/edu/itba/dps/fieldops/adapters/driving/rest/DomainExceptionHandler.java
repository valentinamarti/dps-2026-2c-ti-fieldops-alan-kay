package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ValidationMapper;
import ar.edu.itba.dps.fieldops.business.exceptions.ActivitiesInProgressException;
import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateExpeditionException;
import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateReplanProposalException;
import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateResourceException;
import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateZoneException;
import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotApprovableException;
import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotEditableException;
import ar.edu.itba.dps.fieldops.business.exceptions.InsufficientStockException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidActivityParametersException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidTrackingException;
import ar.edu.itba.dps.fieldops.business.exceptions.NotAResponsibleException;
import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.exceptions.ProposalAlreadyDecidedException;
import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.exceptions.UnacceptedWarningException;
import ar.edu.itba.dps.fieldops.business.exceptions.UnknownActivityTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Map.entry;

@RestControllerAdvice
public class DomainExceptionHandler extends ResponseEntityExceptionHandler {
    static final Map<Class<? extends RuntimeException>, HttpStatus> STATUS_BY_EXCEPTION = Map.ofEntries(
            entry(IllegalArgumentException.class, HttpStatus.BAD_REQUEST),
            entry(NullPointerException.class, HttpStatus.BAD_REQUEST),
            entry(InvalidScheduleException.class, HttpStatus.BAD_REQUEST),
            entry(InvalidTrackingException.class, HttpStatus.BAD_REQUEST),
            entry(UnknownActivityTypeException.class, HttpStatus.BAD_REQUEST),
            entry(InvalidActivityParametersException.class, HttpStatus.BAD_REQUEST),

            entry(NotFoundException.class, HttpStatus.NOT_FOUND),

            entry(InvalidStatusTransitionException.class, HttpStatus.CONFLICT),
            entry(ExpeditionNotEditableException.class, HttpStatus.CONFLICT),
            entry(ActivitiesInProgressException.class, HttpStatus.CONFLICT),
            entry(ResourceUnavailableException.class, HttpStatus.CONFLICT),
            entry(DuplicateExpeditionException.class, HttpStatus.CONFLICT),
            entry(DuplicateResourceException.class, HttpStatus.CONFLICT),
            entry(DuplicateZoneException.class, HttpStatus.CONFLICT),
            entry(DuplicateReplanProposalException.class, HttpStatus.CONFLICT),
            entry(ProposalAlreadyDecidedException.class, HttpStatus.CONFLICT),

            entry(ExpeditionNotApprovableException.class, HttpStatus.UNPROCESSABLE_ENTITY),
            entry(UnacceptedWarningException.class, HttpStatus.UNPROCESSABLE_ENTITY),
            entry(InsufficientStockException.class, HttpStatus.UNPROCESSABLE_ENTITY),
            entry(NotAResponsibleException.class, HttpStatus.UNPROCESSABLE_ENTITY));

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handle(RuntimeException exception) {
        return problemFor(exception);
    }

    @ExceptionHandler(ExpeditionNotApprovableException.class)
    public ProblemDetail handle(ExpeditionNotApprovableException exception) {
        final var problem = problemFor(exception);
        problem.setProperty("criticals", exception.getCriticals().stream().map(ValidationMapper::toDto).toList());
        return problem;
    }

    private ProblemDetail problemFor(RuntimeException exception) {
        return statusOf(exception.getClass())
                .map(status -> problem(status, exception.getMessage()))
                .orElseGet(() -> unexpected(exception));
    }

    static Set<Class<? extends RuntimeException>> mappedExceptions() {
        return STATUS_BY_EXCEPTION.keySet();
    }

    private static Optional<HttpStatus> statusOf(Class<?> type) {
        for (var candidate = type; candidate != null; candidate = candidate.getSuperclass()) {
            final var status = STATUS_BY_EXCEPTION.get(candidate);
            if (status != null) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        final var problem = ProblemDetail.forStatusAndDetail(status,
                Objects.requireNonNullElse(detail, status.getReasonPhrase()));
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }

    private ProblemDetail unexpected(RuntimeException exception) {
        logger.error("Unmapped exception while serving a request", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed");
    }
}
