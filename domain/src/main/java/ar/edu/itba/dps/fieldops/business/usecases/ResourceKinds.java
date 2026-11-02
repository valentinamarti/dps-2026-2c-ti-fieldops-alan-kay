package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.ResourceVisitor;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;

import java.util.List;
import java.util.Optional;

final class ResourceKinds {
    static final ResourceVisitor<Optional<Vehicle>> VEHICLES = new OnlyOneKind<>() {
        @Override
        public Optional<Vehicle> vehicle(Vehicle vehicle) {
            return Optional.of(vehicle);
        }
    };
    static final ResourceVisitor<Optional<Instrument>> INSTRUMENTS = new OnlyOneKind<>() {
        @Override
        public Optional<Instrument> instrument(Instrument instrument) {
            return Optional.of(instrument);
        }
    };
    static final ResourceVisitor<Optional<Depletable>> SUPPLIES = new OnlyOneKind<>() {
        @Override
        public Optional<Depletable> supply(Depletable supply) {
            return Optional.of(supply);
        }
    };

    private ResourceKinds() {
    }

    static <T> List<T> only(ResourceVisitor<Optional<T>> kind, List<? extends Resource> resources) {
        return resources.stream().flatMap(resource -> resource.accept(kind).stream()).toList();
    }

    private abstract static class OnlyOneKind<T> implements ResourceVisitor<Optional<T>> {
        @Override
        public Optional<T> person(Person person) {
            return Optional.empty();
        }

        @Override
        public Optional<T> vehicle(Vehicle vehicle) {
            return Optional.empty();
        }

        @Override
        public Optional<T> instrument(Instrument instrument) {
            return Optional.empty();
        }

        @Override
        public Optional<T> supply(Depletable supply) {
            return Optional.empty();
        }
    }
}
