package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ItineraryNodeResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ItineraryResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduleBlockRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ItineraryMapper;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.usecases.QueryExpeditions;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleBlock;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/expeditions/{expeditionId}")
@Tag(name = "Itinerary", description = "The activities scheduled in an expedition")
public class ItineraryController {
    private final ScheduleBlock scheduleBlock;
    private final QueryExpeditions queryExpeditions;

    public ItineraryController(ScheduleBlock scheduleBlock, QueryExpeditions queryExpeditions) {
        this.scheduleBlock = Objects.requireNonNull(scheduleBlock, "scheduleBlock is required");
        this.queryExpeditions = Objects.requireNonNull(queryExpeditions, "queryExpeditions is required");
    }

    @PostMapping("/blocks")
    @Operation(summary = "Schedules a block of activities, sequential or parallel, with its tree of children")
    @ApiResponse(responseCode = "201", description = "Scheduled, with the block that was created and the "
            + "itinerary it went into in the Location header")
    @ApiResponse(responseCode = "400", description = "A field is missing, the kind of block does not exist, a "
            + "type or a parameter does not serve an activity, or the block does not fit the plan")
    @ApiResponse(responseCode = "404", description = "The expedition, a zone or a dependency does not exist")
    @ApiResponse(responseCode = "409", description = "The expedition is not in draft anymore")
    public ResponseEntity<ItineraryNodeResponse> schedule(@PathVariable String expeditionId,
                                                          @RequestBody ScheduleBlockRequest request) {
        final var block = scheduleBlock.schedule(ItineraryMapper.toCommand(expeditionId, request));
        return ResponseEntity.created(Locations.itineraryOf(expeditionId)).body(ItineraryMapper.toResponse(block));
    }

    @GetMapping("/itinerary")
    @Operation(summary = "Gets the itinerary of an expedition as a tree of activities and blocks")
    @ApiResponse(responseCode = "200", description = "The root nodes, sorted by when they start")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    public ItineraryResponse get(@PathVariable String expeditionId) {
        return itinerary(expeditionId);
    }

    private ItineraryResponse itinerary(String expeditionId) {
        return ItineraryMapper.toResponse(queryExpeditions.get(new ExpeditionId(expeditionId)));
    }
}
