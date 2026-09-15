package ar.edu.itba.dps.fieldops.business.models.zones;

import lombok.Getter;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Zone {

    @Getter
    private final String id;
    @Getter
    private final String name;
    private final Set<Permit> requiredPermits;

    public Zone(String id, String name, Set<Permit> requiredPermits) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.requiredPermits = Set.copyOf(requiredPermits);
    }

    public Set<Permit> missingPermits(Collection<Permit> grantedPermits) {
        return requiredPermits.stream()
                .filter(permit -> !grantedPermits.contains(permit))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAccessibleWith(Collection<Permit> grantedPermits) {
        return missingPermits(grantedPermits).isEmpty();
    }
}
