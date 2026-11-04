package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RecordedEventResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.EventLogMapper;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryExpeditionEventsPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
@Tag(name = "Event log", description = "Reading what happened in an expedition and how each consumer received it")
public class EventLogController {
    private final QueryExpeditionEventsPort queryExpeditionEvents;

    public EventLogController(QueryExpeditionEventsPort queryExpeditionEvents) {
        this.queryExpeditionEvents = Objects.requireNonNull(queryExpeditionEvents, "queryExpeditionEvents is required");
    }

    @GetMapping("/expeditions/{expeditionId}/events")
    @Operation(summary = "Lists the events of an expedition, in the order the system recorded them")
    @ApiResponse(responseCode = "200", description = "The timeline, with a line per consumer of each event: its "
            + "status, how many attempts it took and the error of the last one that failed")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    public List<RecordedEventResponse> ofExpedition(@PathVariable String expeditionId) {
        return queryExpeditionEvents.ofExpedition(new ExpeditionId(expeditionId)).stream()
                .map(EventLogMapper::toResponse)
                .toList();
    }
}
