package ar.edu.itba.dps.fieldops.business.interfaces.replanning;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ResourceAssignment;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.time.Duration;

public interface ReplanReasonVisitor<T> {
    T cancellation();

    T delay(Duration delay);

    T resourceWithdrawn(ResourceId resource);

    T resourceReplaced(ResourceId withdrawn, ResourceAssignment replacement);
}
