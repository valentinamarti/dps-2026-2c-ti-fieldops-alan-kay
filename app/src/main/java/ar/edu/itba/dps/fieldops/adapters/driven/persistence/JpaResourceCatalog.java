package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateResourceException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.ResourceVisitor;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.CalendarSnapshot;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.Certifications;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.Reservation;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.ScheduledResource;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
public class JpaResourceCatalog implements ResourceCatalog {
    private static final String PERSON = "PERSON";
    private static final String VEHICLE = "VEHICLE";
    private static final String INSTRUMENT = "INSTRUMENT";
    private static final String SUPPLY = "SUPPLY";
    private static final String CACHE_PREFIX = "resource:";

    private final ResourceJpaRepository resources;
    private final LoadedAggregates loaded;

    public JpaResourceCatalog(ResourceJpaRepository resources, LoadedAggregates loaded) {
        this.resources = Objects.requireNonNull(resources, "resources are required");
        this.loaded = Objects.requireNonNull(loaded, "loaded is required");
    }

    @Override
    public void addStaff(Person person) {
        add(person);
    }

    @Override
    public void addEquipment(Equipment item) {
        add(item);
    }

    @Override
    public void addSupply(DepletableResource supply) {
        add(supply);
    }

    @Override
    public void save(Resource resource) {
        Objects.requireNonNull(resource, "resource is required");
        final var entity = resources.findById(resource.getId().value()).orElseGet(ResourceEntity::new);
        resource.accept(new Filling(entity));
        resources.save(entity);
        loaded.put(CACHE_PREFIX + resource.getId().value(), resource);
    }

    @Override
    public Optional<Person> findStaff(ResourceId id) {
        return findOfKinds(id, List.of(PERSON)).map(Person.class::cast);
    }

    @Override
    public Optional<Equipment> findEquipment(ResourceId id) {
        return findOfKinds(id, List.of(VEHICLE, INSTRUMENT)).map(Equipment.class::cast);
    }

    @Override
    public Optional<DepletableResource> findSupply(ResourceId id) {
        return findOfKinds(id, List.of(SUPPLY)).map(DepletableResource.class::cast);
    }

    @Override
    public List<Person> allStaff() {
        return allOfKinds(List.of(PERSON)).stream().map(Person.class::cast).toList();
    }

    @Override
    public List<Equipment> allEquipment() {
        return allOfKinds(List.of(VEHICLE, INSTRUMENT)).stream().map(Equipment.class::cast).toList();
    }

    @Override
    public List<DepletableResource> allSupplies() {
        return allOfKinds(List.of(SUPPLY)).stream().map(DepletableResource.class::cast).toList();
    }

    @Override
    public List<Person> availableStaffFor(StaffRequirement requirement, TimePeriod period) {
        return ResourceCatalog.super.availableStaffFor(requirement, period);
    }

    @Override
    public List<Equipment> availableEquipmentFor(ReusableRequirement requirement, TimePeriod period) {
        return ResourceCatalog.super.availableEquipmentFor(requirement, period);
    }

    @Override
    public List<DepletableResource> suppliesCovering(DepletableRequirement requirement) {
        return ResourceCatalog.super.suppliesCovering(requirement);
    }

    private void add(Resource resource) {
        Objects.requireNonNull(resource, "resource is required");
        if (resources.existsById(resource.getId().value())) {
            throw new DuplicateResourceException(resource.getId());
        }
        save(resource);
    }

    private Optional<Resource> findOfKinds(ResourceId id, List<String> kinds) {
        return resources.findById(id.value()).filter(entity -> kinds.contains(entity.getKind())).map(this::loadedFrom);
    }

    private List<Resource> allOfKinds(List<String> kinds) {
        return resources.findAllByKindInOrderByCreatedOrderAsc(kinds).stream().map(this::loadedFrom).toList();
    }

    private Resource loadedFrom(ResourceEntity entity) {
        final var key = CACHE_PREFIX + entity.getId();
        final Resource alreadyLoaded = loaded.get(key);
        if (alreadyLoaded != null) {
            return alreadyLoaded;
        }
        final var resource = toDomain(entity);
        loaded.put(key, resource);
        return resource;
    }

    private Resource toDomain(ResourceEntity entity) {
        final var id = new ResourceId(entity.getId());
        return switch (entity.getKind()) {
            case PERSON -> Person.restored(id, entity.getName(), certificationsOf(entity), calendarOf(entity));
            case VEHICLE -> Vehicle.restored(id, entity.getName(), new ResourceCategory(entity.getCategory()),
                    entity.getLicensePlate(), certificationsOf(entity), calendarOf(entity));
            case INSTRUMENT -> Instrument.restored(id, entity.getName(), new ResourceCategory(entity.getCategory()),
                    certificationsOf(entity), calendarOf(entity));
            case SUPPLY -> Depletable.restored(id, entity.getName(), new ResourceCategory(entity.getCategory()),
                    new Quantity(entity.getStockAmount(), MeasurementUnit.valueOf(entity.getStockUnit())), reservationsOf(entity));
            default -> throw new IllegalStateException("unknown kind of resource: " + entity.getKind());
        };
    }

    private Set<Certification> certificationsOf(ResourceEntity entity) {
        return entity.getCertifications().stream().map(Certification::new).collect(Collectors.toUnmodifiableSet());
    }

    private CalendarSnapshot calendarOf(ResourceEntity entity) {
        return new CalendarSnapshot(
                entity.getReservations().stream()
                        .map(row -> new Reservation(new ExpeditionId(row.getExpeditionId()), new TimePeriod(row.getStartAt(), row.getEndAt())))
                        .toList(),
                entity.getUnavailabilities().stream()
                        .map(row -> new Unavailability(new TimePeriod(row.getStartAt(), row.getEndAt()), row.getReason()))
                        .toList());
    }

    private Map<ExpeditionId, Quantity> reservationsOf(ResourceEntity entity) {
        return entity.getSupplyReservations().stream().collect(Collectors.toMap(
                row -> new ExpeditionId(row.getExpeditionId()),
                row -> new Quantity(row.getAmount(), MeasurementUnit.valueOf(row.getUnit()))));
    }

    private static final class Filling implements ResourceVisitor<Void> {
        private final ResourceEntity entity;

        private Filling(ResourceEntity entity) {
            this.entity = entity;
        }

        @Override
        public Void person(Person person) {
            common(person, PERSON, person.getName());
            certifications(person.getCertifications());
            calendar(person);
            return null;
        }

        @Override
        public Void vehicle(Vehicle vehicle) {
            common(vehicle, VEHICLE, vehicle.getName());
            entity.setCategory(vehicle.getCategory().name());
            entity.setLicensePlate(vehicle.getLicensePlate());
            certifications(vehicle.getCertifications());
            calendar(vehicle);
            return null;
        }

        @Override
        public Void instrument(Instrument instrument) {
            common(instrument, INSTRUMENT, instrument.getName());
            entity.setCategory(instrument.getCategory().name());
            certifications(instrument.getCertifications());
            calendar(instrument);
            return null;
        }

        @Override
        public Void supply(Depletable supply) {
            common(supply, SUPPLY, supply.getName());
            entity.setCategory(supply.getCategory().name());
            entity.setStockAmount(supply.getStock().amount());
            entity.setStockUnit(supply.getStock().unit().name());
            entity.getSupplyReservations().clear();
            supply.reservations().forEach((owner, quantity) -> entity.getSupplyReservations()
                    .add(new SupplyReservationRow(owner.value(), quantity.amount(), quantity.unit().name())));
            return null;
        }

        private void common(Resource resource, String kind, String name) {
            entity.setId(resource.getId().value());
            entity.setKind(kind);
            entity.setName(name);
        }

        private void certifications(Certifications certifications) {
            entity.getCertifications().clear();
            certifications.values().forEach(certification -> entity.getCertifications().add(certification.name()));
        }

        private void calendar(ScheduledResource resource) {
            final var calendar = resource.calendarSnapshot();
            entity.getReservations().clear();
            calendar.reservations().forEach(reservation -> entity.getReservations().add(new ReservationRow(
                    reservation.owner().value(), reservation.period().start(), reservation.period().end())));
            entity.getUnavailabilities().clear();
            calendar.unavailabilities().forEach(unavailability -> entity.getUnavailabilities().add(new UnavailabilityRow(
                    unavailability.period().start(), unavailability.period().end(), unavailability.reason())));
        }
    }
}
