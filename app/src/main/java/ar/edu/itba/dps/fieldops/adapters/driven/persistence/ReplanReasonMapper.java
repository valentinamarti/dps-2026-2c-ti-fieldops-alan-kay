package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReasonVisitor;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ResourceAssignment;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.replanning.Cancellation;
import ar.edu.itba.dps.fieldops.business.replanning.Delay;
import ar.edu.itba.dps.fieldops.business.replanning.ResourceReplaced;
import ar.edu.itba.dps.fieldops.business.replanning.ResourceWithdrawn;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

class ReplanReasonMapper {
    private static final String CANCELLATION = "CANCELLATION";
    private static final String DELAY = "DELAY";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final String REPLACED = "REPLACED";

    private static final ReplanReasonVisitor<ReasonColumns> AS_COLUMNS = new ReplanReasonVisitor<>() {
        @Override
        public ReasonColumns cancellation() {
            return new ReasonColumns(CANCELLATION, null, null, null);
        }

        @Override
        public ReasonColumns delay(Duration delay) {
            return new ReasonColumns(DELAY, delay.toMinutes(), null, null);
        }

        @Override
        public ReasonColumns resourceWithdrawn(ResourceId resource) {
            return new ReasonColumns(WITHDRAWN, null, resource.value(), null);
        }

        @Override
        public ReasonColumns resourceReplaced(ResourceId withdrawn, ResourceAssignment replacement) {
            return new ReasonColumns(REPLACED, null, withdrawn.value(), replacement.resource().getId().value());
        }
    };

    private final ResourceCatalog catalog;

    ReplanReasonMapper(ResourceCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    ReasonColumns toColumns(ReplanReason reason) {
        return reason.accept(AS_COLUMNS);
    }

    ReplanReason toDomain(ReasonColumns columns) {
        return switch (columns.getKind()) {
            case CANCELLATION -> new Cancellation();
            case DELAY -> new Delay(Duration.ofMinutes(columns.getDelayMinutes()));
            case WITHDRAWN -> new ResourceWithdrawn(new ResourceId(columns.getWithdrawn()));
            case REPLACED -> new ResourceReplaced(new ResourceId(columns.getWithdrawn()), assignmentOf(new ResourceId(columns.getReplacement())));
            default -> throw new IllegalStateException("unknown replan reason: " + columns.getKind());
        };
    }

    private ResourceAssignment assignmentOf(ResourceId id) {
        return catalog.findStaff(id).map(ResourceAssignment::of)
                .or(() -> catalog.findEquipment(id).map(ResourceAssignment::of))
                .or(() -> catalog.findSupply(id).map(ResourceAssignment::of))
                .orElseThrow(() -> new IllegalStateException("the resource %s is not registered".formatted(id)));
    }

    static Optional<ReasonColumns> present(ReasonColumns columns) {
        return Optional.ofNullable(columns).filter(reason -> reason.getKind() != null);
    }
}
