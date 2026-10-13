package ar.edu.itba.dps.fieldops.business.models.zones;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import lombok.Getter;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class Zone {
    private static final String SPECIAL_PERMIT_NAME = "%s in zone %s";

    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final Set<Permit> requiredPermits;

    public Zone(String id, String name, Set<Permit> requiredPermits) {
        this.id = DomainArguments.requireText(id, "id");
        this.name = DomainArguments.requireText(name, "name");
        this.requiredPermits = DomainArguments.requireSet(requiredPermits, "requiredPermits");
    }

    public Set<Permit> missingPermits(Collection<Permit> grantedPermits) {
        return requiredPermits.stream()
                .filter(permit -> !grantedPermits.contains(permit))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAccessibleWith(Collection<Permit> grantedPermits) {
        return missingPermits(grantedPermits).isEmpty();
    }

    public Permit specialPermitFor(String operation) {
        final var normalized = DomainArguments.requireText(operation, "operation").strip().toLowerCase(Locale.ROOT);
        return new Permit(SPECIAL_PERMIT_NAME.formatted(normalized, id));
    }
}
