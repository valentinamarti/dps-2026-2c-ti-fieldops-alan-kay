package ar.edu.itba.dps.fieldops.adapters.driving.rest;

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
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityTypeName;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static ar.edu.itba.dps.fieldops.business.models.common.Quantity.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error mapping of the plan of the Fase 5, exception by exception: a controller that only throws, the real
 * advice, and the status code and the body that come out the other end.
 */
class DomainExceptionHandlerTest {

    private static final String EXCEPTIONS_PACKAGE = "ar.edu.itba.dps.fieldops.business.exceptions";

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("theErrorMapping")
    void answersTheStatusOfTheMappingWithTheMessageOfTheDomain(RuntimeException failure, HttpStatus expected) throws Exception {
        apiThrowing(failure).perform(get("/boom"))
                .andExpect(status().is(expected.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expected.value()))
                .andExpect(jsonPath("$.title").value(expected.getReasonPhrase()))
                .andExpect(jsonPath("$.detail").value(failure.getMessage()));
    }

    @Test
    void answersAServerErrorWithoutLeakingTheMessageWhenTheFailureIsNotInTheMapping() throws Exception {
        apiThrowing(new IllegalStateException("the repository is on fire")).perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("The request could not be completed"));
    }

    /**
     * Guards the mapping against a domain exception that nobody classified: a new one in {@code
     * business.exceptions} fails here instead of silently answering a 500.
     */
    @Test
    void mapsEveryExceptionOfTheDomain() {
        final var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(RuntimeException.class));

        final var declared = scanner.findCandidateComponents(EXCEPTIONS_PACKAGE).stream()
                .map(definition -> Objects.requireNonNull(definition.getBeanClassName()))
                .sorted()
                .toList();

        assertThat(declared).isNotEmpty();
        assertThat(DomainExceptionHandler.mappedExceptions()).map(Class::getName).containsAll(declared);
    }

    private static MockMvc apiThrowing(RuntimeException failure) {
        return MockMvcBuilders.standaloneSetup(new AlwaysFailingController(failure))
                .setControllerAdvice(new DomainExceptionHandler())
                .build();
    }

    private static Stream<Arguments> theErrorMapping() {
        final var expedition = new ExpeditionId("e-1");
        final var person = new ResourceId("p-1");
        final var proposal = new ReplanProposalId("r-1");
        final var subject = ValidationSubject.expedition();
        final var critical = ValidationResult.critical(new RuleId("missing-permits"), subject, "The night permit is missing");
        final var warning = ValidationResult.warning(new RuleId("risk-tolerance"), subject, "The risk is above the tolerance");
        return Stream.of(
                arguments(new IllegalArgumentException("name is required"), HttpStatus.BAD_REQUEST),
                arguments(new NullPointerException("period is required"), HttpStatus.BAD_REQUEST),
                arguments(new InvalidScheduleException("it is scheduled outside the expedition period"), HttpStatus.BAD_REQUEST),
                arguments(new InvalidTrackingException("the expedition is not in execution"), HttpStatus.BAD_REQUEST),
                arguments(new UnknownActivityTypeException(new ActivityTypeName("spelunking"),
                        List.of(new ActivityTypeName("diving"))), HttpStatus.BAD_REQUEST),
                arguments(new InvalidActivityParametersException(new ActivityTypeName("diving"),
                        "maxDepthMeters is required"), HttpStatus.BAD_REQUEST),

                arguments(new NotFoundException("expedition", expedition), HttpStatus.NOT_FOUND),

                arguments(new InvalidStatusTransitionException(ExpeditionStatus.DRAFT, ExpeditionStatus.APPROVED), HttpStatus.CONFLICT),
                arguments(new ExpeditionNotEditableException(ExpeditionStatus.APPROVED), HttpStatus.CONFLICT),
                arguments(new ActivitiesInProgressException(List.of("Sampling a-1")), HttpStatus.CONFLICT),
                arguments(new ResourceUnavailableException(hours(8, 18)), HttpStatus.CONFLICT),
                arguments(new DuplicateExpeditionException(expedition), HttpStatus.CONFLICT),
                arguments(new DuplicateResourceException(person), HttpStatus.CONFLICT),
                arguments(new DuplicateZoneException("z-1"), HttpStatus.CONFLICT),
                arguments(new DuplicateReplanProposalException(proposal), HttpStatus.CONFLICT),
                arguments(new ProposalAlreadyDecidedException(proposal, ReplanProposalStatus.ACCEPTED), HttpStatus.CONFLICT),

                arguments(new ExpeditionNotApprovableException(List.of(critical)), HttpStatus.UNPROCESSABLE_ENTITY),
                arguments(new UnacceptedWarningException(warning), HttpStatus.UNPROCESSABLE_ENTITY),
                arguments(new InsufficientStockException(new ResourceId("j-1"), of("10", UNITS)), HttpStatus.UNPROCESSABLE_ENTITY),
                arguments(new NotAResponsibleException(person, expedition), HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @RestController
    private record AlwaysFailingController(RuntimeException failure) {

        @GetMapping("/boom")
        String boom() {
            throw failure;
        }
    }
}
