package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityDefinition;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.BlockKind;
import ar.edu.itba.dps.fieldops.business.models.expeditions.BlockSnapshot;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionSnapshot;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItemSnapshot;
import ar.edu.itba.dps.fieldops.business.models.expeditions.NodeSnapshot;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Observation;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ReplacedPlan;
import ar.edu.itba.dps.fieldops.business.models.expeditions.TrackingSnapshot;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.Severity;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class ExpeditionMapper {
    private static final String ITEM = "ITEM";
    private static final String STAFF = "STAFF";
    private static final String EQUIPMENT = "EQUIPMENT";
    private static final String SUPPLY = "SUPPLY";

    private final ZoneRepository zones;
    private final ResourceCatalog catalog;
    private final ActivityTypeRegistry types;
    private final ReplanReasonMapper reasons;

    ExpeditionMapper(ZoneRepository zones, ResourceCatalog catalog, ActivityTypeRegistry types) {
        this.zones = Objects.requireNonNull(zones, "zones are required");
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
        this.types = Objects.requireNonNull(types, "types are required");
        this.reasons = new ReplanReasonMapper(catalog);
    }

    void fill(ExpeditionEntity entity, ExpeditionSnapshot snapshot) {
        entity.setId(snapshot.id().value());
        entity.setName(snapshot.name());
        entity.setPeriodStart(snapshot.period().start());
        entity.setPeriodEnd(snapshot.period().end());
        entity.setMaxParticipants(snapshot.restrictions().maxParticipants());
        entity.setRiskTolerance(snapshot.restrictions().riskTolerance().name());
        entity.setStatus(snapshot.status().name());
        entity.setReplacedExpedition(snapshot.replacedPlan().map(plan -> plan.expedition().value()).orElse(null));
        entity.setReplacedRevision(snapshot.replacedPlan().map(ReplacedPlan::executionRevision).orElse(null));
        entity.setReason(snapshot.replanReason().map(reasons::toColumns).orElse(null));
        entity.setReplacement(snapshot.replacement().map(ExpeditionId::value).orElse(null));
        entity.setExecutionRevision(snapshot.executionRevision());
        replace(entity.getObjectives(), snapshot.objectives());
        replace(entity.getZoneIds(), snapshot.zones().stream().map(Zone::getId).toList());
        replace(entity.getResponsibleIds(), snapshot.responsibles().stream().map(person -> person.getId().value()).toList());
        replace(entity.getPermits(), snapshot.grantedPermits().stream().map(Permit::name).toList());
        replace(entity.getAcceptedWarnings(), snapshot.acceptedWarnings().stream().map(this::toRow).toList());
        replace(entity.getIncidents(), snapshot.incidents().stream().map(IncidentMapper::toColumns).toList());
        entity.getActivities().clear();
        entity.getParameters().clear();
        entity.getDependencies().clear();
        entity.getNodes().clear();
        entity.getAssignments().clear();
        entity.getObservations().clear();
        final var activities = new LinkedHashMap<String, Activity>();
        snapshot.roots().forEach(root -> flatten(entity, root, null, activities));
        activities.values().forEach(activity -> addActivity(entity, activity));
    }

    Expedition toDomain(ExpeditionEntity entity) {
        final var activities = activitiesOf(entity);
        return Expedition.restore(new ExpeditionSnapshot(
                new ExpeditionId(entity.getId()),
                entity.getName(),
                new TimePeriod(entity.getPeriodStart(), entity.getPeriodEnd()),
                List.copyOf(entity.getObjectives()),
                entity.getZoneIds().stream().map(this::zone).collect(Collectors.toUnmodifiableSet()),
                entity.getResponsibleIds().stream().map(id -> person(new ResourceId(id))).collect(Collectors.toUnmodifiableSet()),
                new ExpeditionRestrictions(entity.getMaxParticipants(), RiskLevel.valueOf(entity.getRiskTolerance())),
                entity.getPermits().stream().map(Permit::new).collect(Collectors.toUnmodifiableSet()),
                ExpeditionStatus.valueOf(entity.getStatus()),
                entity.getAcceptedWarnings().stream().map(this::toDomain).toList(),
                entity.getIncidents().stream().map(IncidentMapper::toDomain).toList(),
                replacedPlanOf(entity),
                ReplanReasonMapper.present(entity.getReason()).map(reasons::toDomain),
                Optional.ofNullable(entity.getReplacement()).map(ExpeditionId::new),
                entity.getExecutionRevision(),
                rootsOf(entity, activities)));
    }

    private static <T> void replace(Collection<T> target, Collection<T> values) {
        target.clear();
        target.addAll(values);
    }

    private void flatten(ExpeditionEntity entity, NodeSnapshot node, Integer parent, Map<String, Activity> activities) {
        switch (node) {
            case ItemSnapshot item -> addItem(entity, item, parent, activities);
            case BlockSnapshot block -> {
                final var position = entity.getNodes().size();
                entity.getNodes().add(new ItineraryNodeRow(parent, block.kind().name(), null, null, null, null, null, null));
                block.children().forEach(child -> flatten(entity, child, position, activities));
            }
        }
    }

    private void addItem(ExpeditionEntity entity, ItemSnapshot item, Integer parent, Map<String, Activity> activities) {
        final var activityId = item.activity().getId().value();
        collect(item.activity(), activities);
        entity.getNodes().add(new ItineraryNodeRow(parent, ITEM, activityId,
                item.scheduledPeriod().start(), item.scheduledPeriod().end(),
                item.tracking().map(TrackingSnapshot::startedAt).orElse(null),
                item.tracking().flatMap(TrackingSnapshot::finishedAt).orElse(null),
                item.tracking().flatMap(TrackingSnapshot::result).orElse(null)));
        addAssignments(entity, activityId, STAFF, item.staff());
        addAssignments(entity, activityId, EQUIPMENT, item.equipment());
        addAssignments(entity, activityId, SUPPLY, item.supplies());
        item.tracking().map(TrackingSnapshot::observations).orElse(List.of()).forEach(observation -> entity.getObservations()
                .add(new NodeObservationRow(activityId, observation.note(), observation.recordedAt())));
    }

    private void addAssignments(ExpeditionEntity entity, String activityId, String role, List<? extends Resource> resources) {
        resources.forEach(resource -> entity.getAssignments().add(new NodeAssignmentRow(activityId, role, resource.getId().value())));
    }

    private void collect(Activity activity, Map<String, Activity> activities) {
        if (activities.putIfAbsent(activity.getId().value(), activity) == null) {
            activity.getDependencies().forEach(dependency -> collect(dependency, activities));
        }
    }

    private void addActivity(ExpeditionEntity entity, Activity activity) {
        final var id = activity.getId().value();
        final var definition = activity.definition();
        entity.getActivities().add(new ActivityRow(id, activity.getName(), activity.getTimeWindow().start(),
                activity.getTimeWindow().end(), activity.getZone().getId(), definition.typeName(), definition.atNight()));
        definition.parameters().forEach((name, value) -> entity.getParameters().add(new ActivityParameterRow(id, name, value)));
        activity.getDependencies().forEach(dependency -> entity.getDependencies().add(new ActivityDependencyRow(id, dependency.getId().value())));
    }

    private AcceptedWarningRow toRow(AcceptedWarning accepted) {
        final var warning = accepted.warning();
        return new AcceptedWarningRow(warning.severity().name(), warning.rule().value(),
                warning.subject().activity().map(ActivityId::value).orElse(null),
                warning.subject().resource().map(ResourceId::value).orElse(null),
                warning.message(), accepted.responsible().getId().value(), accepted.justification());
    }

    private AcceptedWarning toDomain(AcceptedWarningRow row) {
        final var subject = new ValidationSubject(
                Optional.ofNullable(row.getSubjectActivity()).map(ActivityId::new),
                Optional.ofNullable(row.getSubjectResource()).map(ResourceId::new));
        final var warning = new ValidationResult(Severity.valueOf(row.getSeverity()), new RuleId(row.getRule()), subject, row.getMessage());
        return new AcceptedWarning(warning, person(new ResourceId(row.getResponsibleId())), row.getJustification());
    }

    private Optional<ReplacedPlan> replacedPlanOf(ExpeditionEntity entity) {
        return Optional.ofNullable(entity.getReplacedExpedition())
                .map(id -> new ReplacedPlan(new ExpeditionId(id), entity.getReplacedRevision()));
    }

    private Map<String, Activity> activitiesOf(ExpeditionEntity entity) {
        final var rows = entity.getActivities().stream().collect(Collectors.toMap(ActivityRow::getActivityId, row -> row));
        final var built = new HashMap<String, Activity>();
        rows.keySet().forEach(id -> build(id, rows, entity, built));
        return built;
    }

    private Activity build(String id, Map<String, ActivityRow> rows, ExpeditionEntity entity, Map<String, Activity> built) {
        if (built.containsKey(id)) {
            return built.get(id);
        }
        final var row = rows.get(id);
        final var dependencies = entity.getDependencies().stream()
                .filter(dependency -> dependency.getActivityId().equals(id))
                .map(dependency -> build(dependency.getDependsOn(), rows, entity, built))
                .toList();
        final var parameters = entity.getParameters().stream()
                .filter(parameter -> parameter.getActivityId().equals(id))
                .collect(Collectors.toMap(ActivityParameterRow::getName, ActivityParameterRow::getValue));
        final var activity = new Activity(new ActivityId(id), row.getName(), new TimePeriod(row.getWindowStart(), row.getWindowEnd()),
                zone(row.getZoneId()), dependencies, types.rulesFor(new ActivityDefinition(row.getTypeName(), parameters, row.isAtNight())));
        built.put(id, activity);
        return activity;
    }

    private List<NodeSnapshot> rootsOf(ExpeditionEntity entity, Map<String, Activity> activities) {
        return childrenOf(null, entity, activities);
    }

    private List<NodeSnapshot> childrenOf(Integer parent, ExpeditionEntity entity, Map<String, Activity> activities) {
        final var nodes = entity.getNodes();
        return IntStream.range(0, nodes.size())
                .filter(position -> Objects.equals(nodes.get(position).getParentPosition(), parent))
                .mapToObj(position -> nodeAt(position, entity, activities))
                .toList();
    }

    private NodeSnapshot nodeAt(int position, ExpeditionEntity entity, Map<String, Activity> activities) {
        final var row = entity.getNodes().get(position);
        return ITEM.equals(row.getKind())
                ? itemOf(row, entity, activities)
                : new BlockSnapshot(BlockKind.valueOf(row.getKind()), childrenOf(position, entity, activities));
    }

    private ItemSnapshot itemOf(ItineraryNodeRow row, ExpeditionEntity entity, Map<String, Activity> activities) {
        final var activityId = row.getActivityId();
        return new ItemSnapshot(
                activities.get(activityId),
                new TimePeriod(row.getStartAt(), row.getEndAt()),
                idsAssigned(entity, activityId, STAFF).stream().map(this::person).toList(),
                idsAssigned(entity, activityId, EQUIPMENT).stream().map(id -> catalog.findEquipment(id).orElseThrow(() -> notRegistered(id))).toList(),
                idsAssigned(entity, activityId, SUPPLY).stream().map(id -> catalog.findSupply(id).orElseThrow(() -> notRegistered(id))).toList(),
                Optional.ofNullable(row.getStartedAt()).map(startedAt -> new TrackingSnapshot(startedAt,
                        Optional.ofNullable(row.getFinishedAt()), Optional.ofNullable(row.getResult()), observationsOf(entity, activityId))));
    }

    private List<ResourceId> idsAssigned(ExpeditionEntity entity, String activityId, String role) {
        return entity.getAssignments().stream()
                .filter(assignment -> assignment.getActivityId().equals(activityId) && assignment.getRole().equals(role))
                .map(assignment -> new ResourceId(assignment.getResourceId()))
                .toList();
    }

    private List<Observation> observationsOf(ExpeditionEntity entity, String activityId) {
        return entity.getObservations().stream()
                .filter(observation -> observation.getActivityId().equals(activityId))
                .map(observation -> new Observation(observation.getNote(), observation.getRecordedAt()))
                .toList();
    }

    private Zone zone(String id) {
        return zones.findById(id).orElseThrow(() -> new IllegalStateException("the zone %s is not registered".formatted(id)));
    }

    private Person person(ResourceId id) {
        return catalog.findStaff(id).orElseThrow(() -> notRegistered(id));
    }

    private IllegalStateException notRegistered(ResourceId id) {
        return new IllegalStateException("the resource %s is not registered".formatted(id));
    }
}
