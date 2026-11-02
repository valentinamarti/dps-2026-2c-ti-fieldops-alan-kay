package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;

import java.util.List;
import java.util.function.Function;

public enum BlockKind {
    SEQUENTIAL(SequentialBlock::new),
    PARALLEL(ParallelBlock::new);

    private final Function<List<ItineraryNode>, ItineraryNode> assembler;

    BlockKind(Function<List<ItineraryNode>, ItineraryNode> assembler) {
        this.assembler = assembler;
    }

    ItineraryNode assemble(List<ItineraryNode> children) {
        return assembler.apply(children);
    }
}
