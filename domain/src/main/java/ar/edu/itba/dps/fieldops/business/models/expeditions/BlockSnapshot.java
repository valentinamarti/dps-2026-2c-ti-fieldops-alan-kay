package ar.edu.itba.dps.fieldops.business.models.expeditions;

import java.util.List;
import java.util.Objects;

public record BlockSnapshot(BlockKind kind, List<NodeSnapshot> children) implements NodeSnapshot {

    public BlockSnapshot {
        Objects.requireNonNull(kind, "kind is required");
        children = List.copyOf(children);
    }
}
