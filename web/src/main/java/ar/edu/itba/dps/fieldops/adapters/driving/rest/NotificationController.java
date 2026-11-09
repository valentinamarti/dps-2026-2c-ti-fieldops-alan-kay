package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.NotificationResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.NotificationMapper;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryNotificationsPort;
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
@Tag(name = "Notifications", description = "Reading what the responsibles of an expedition were told")
public class NotificationController {
    private final QueryNotificationsPort queryNotifications;

    public NotificationController(QueryNotificationsPort queryNotifications) {
        this.queryNotifications = Objects.requireNonNull(queryNotifications, "queryNotifications is required");
    }

    @GetMapping("/expeditions/{expeditionId}/notifications")
    @Operation(summary = "Lists the notifications of an expedition, in the order they arrived")
    @ApiResponse(responseCode = "200", description = "One notification per responsible and event. They appear "
            + "some seconds after the fact, once the dispatcher delivers the event")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    public List<NotificationResponse> ofExpedition(@PathVariable String expeditionId) {
        return queryNotifications.ofExpedition(new ExpeditionId(expeditionId)).stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }
}
