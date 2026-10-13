package ar.edu.itba.dps.fieldops.adapters.driving.seed;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.CreateExpeditionRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.PeriodDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterInstrumentRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterStaffRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterSupplyRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterVehicleRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterZoneRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduleBlockRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.UnavailabilityDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ActivityMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ExpeditionMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.InstrumentMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ItineraryMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.StaffMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.SupplyMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.UnavailabilityMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.VehicleMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ZoneMapper;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;
import ar.edu.itba.dps.fieldops.business.usecases.AssignResources;
import ar.edu.itba.dps.fieldops.business.usecases.CreateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.MarkResourceUnavailable;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterResources;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterZone;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleActivity;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleBlock;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class DemoDataLoader {
    private final RegisterZone registerZone;
    private final RegisterResources registerResources;
    private final MarkResourceUnavailable markResourceUnavailable;
    private final CreateExpedition createExpedition;
    private final ScheduleActivity scheduleActivity;
    private final ScheduleBlock scheduleBlock;
    private final AssignResources assignResources;
    private final ObjectMapper json;

    public DemoDataLoader(RegisterZone registerZone, RegisterResources registerResources,
                          MarkResourceUnavailable markResourceUnavailable, CreateExpedition createExpedition,
                          ScheduleActivity scheduleActivity, ScheduleBlock scheduleBlock,
                          AssignResources assignResources, ObjectMapper json) {
        this.registerZone = Objects.requireNonNull(registerZone, "registerZone is required");
        this.registerResources = Objects.requireNonNull(registerResources, "registerResources is required");
        this.markResourceUnavailable =
                Objects.requireNonNull(markResourceUnavailable, "markResourceUnavailable is required");
        this.createExpedition = Objects.requireNonNull(createExpedition, "createExpedition is required");
        this.scheduleActivity = Objects.requireNonNull(scheduleActivity, "scheduleActivity is required");
        this.scheduleBlock = Objects.requireNonNull(scheduleBlock, "scheduleBlock is required");
        this.assignResources = Objects.requireNonNull(assignResources, "assignResources is required");
        this.json = Objects.requireNonNull(json, "json is required");
    }

    public List<ExpeditionId> load(InputStream catalog, InputStream expeditions) {
        load(read(catalog, Catalog.class));
        return read(expeditions, Expeditions.class).expeditions().stream().map(this::load).toList();
    }

    private void load(Catalog catalog) {
        catalog.zones().forEach(zone -> registerZone.register(ZoneMapper.toCommand(zone)));
        catalog.staff().forEach(person -> registerResources.registerPerson(StaffMapper.toCommand(person)));
        catalog.vehicles().forEach(vehicle -> registerResources.registerVehicle(VehicleMapper.toCommand(vehicle)));
        catalog.instruments()
                .forEach(instrument -> registerResources.registerInstrument(InstrumentMapper.toCommand(instrument)));
        catalog.supplies().forEach(supply -> registerResources.registerSupply(SupplyMapper.toCommand(supply)));
        markUnavailable(catalog.staffUnavailabilities(), markResourceUnavailable::markStaffUnavailable);
        markUnavailable(catalog.vehicleUnavailabilities(), markResourceUnavailable::markVehicleUnavailable);
        markUnavailable(catalog.instrumentUnavailabilities(), markResourceUnavailable::markInstrumentUnavailable);
    }

    private ExpeditionId load(PlannedExpedition planned) {
        final var id = createExpedition.create(ExpeditionMapper.toCommand(planned.expedition())).getId();
        planned.itinerary().forEach(node -> schedule(id, node));
        planned.assignments().forEach(assignment -> assign(id, assignment));
        return id;
    }

    private void schedule(ExpeditionId expedition, ScheduleBlockRequest node) {
        if (node.activity() == null) {
            scheduleBlock.schedule(ItineraryMapper.toCommand(expedition.value(), node));
        } else {
            scheduleActivity.schedule(ActivityMapper.toCommand(expedition.value(), node.activity()));
        }
    }

    private void assign(ExpeditionId expedition, Assignment assignment) {
        final var activity = new ActivityId(assignment.activityId());
        assignment.staffIds().forEach(staff ->
                assignResources.assignStaff(expedition, activity, new ResourceId(staff)));
        assignment.equipmentIds().forEach(equipment ->
                assignResources.assignEquipment(expedition, activity, new ResourceId(equipment)));
        assignment.supplyIds().forEach(supply ->
                assignResources.assignSupply(expedition, activity, new ResourceId(supply)));
    }

    private void markUnavailable(List<ResourceUnavailability> unavailabilities,
                                 BiConsumer<ResourceId, Unavailability> mark) {
        unavailabilities.forEach(unavailability -> mark.accept(new ResourceId(unavailability.resourceId()),
                UnavailabilityMapper.toUnavailability(
                        new UnavailabilityDto(unavailability.period(), unavailability.reason()))));
    }

    private <T> T read(InputStream document, Class<T> shape) {
        Objects.requireNonNull(document, "document is required");
        try (document) {
            return json.readValue(document, shape);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("the demo document %s could not be read".formatted(shape.getSimpleName()),
                    unreadable);
        }
    }

    record Catalog(List<RegisterZoneRequest> zones, List<RegisterStaffRequest> staff,
                   List<RegisterVehicleRequest> vehicles, List<RegisterInstrumentRequest> instruments,
                   List<RegisterSupplyRequest> supplies, List<ResourceUnavailability> staffUnavailabilities,
                   List<ResourceUnavailability> vehicleUnavailabilities,
                   List<ResourceUnavailability> instrumentUnavailabilities) {
        Catalog {
            zones = orEmpty(zones);
            staff = orEmpty(staff);
            vehicles = orEmpty(vehicles);
            instruments = orEmpty(instruments);
            supplies = orEmpty(supplies);
            staffUnavailabilities = orEmpty(staffUnavailabilities);
            vehicleUnavailabilities = orEmpty(vehicleUnavailabilities);
            instrumentUnavailabilities = orEmpty(instrumentUnavailabilities);
        }
    }

    record ResourceUnavailability(String resourceId, PeriodDto period, String reason) {
    }

    record Expeditions(List<PlannedExpedition> expeditions) {
        Expeditions {
            expeditions = orEmpty(expeditions);
        }
    }

    record PlannedExpedition(CreateExpeditionRequest expedition, List<ScheduleBlockRequest> itinerary,
                             List<Assignment> assignments) {
        PlannedExpedition {
            itinerary = orEmpty(itinerary);
            assignments = orEmpty(assignments);
        }
    }

    record Assignment(String activityId, List<String> staffIds, List<String> equipmentIds,
                      List<String> supplyIds) {
        Assignment {
            staffIds = orEmpty(staffIds);
            equipmentIds = orEmpty(equipmentIds);
            supplyIds = orEmpty(supplyIds);
        }
    }

    private static <T> List<T> orEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
