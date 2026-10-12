package ar.edu.itba.dps.fieldops.business.interfaces.itinerary;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;

import java.util.List;

/**
 * An operation whose answer depends on which kind of node of the itinerary it is asked about, like showing the
 * tree outside the domain (Visitor, D54).
 *
 * <p>Each node calls the method that belongs to it, so nobody asks of what kind a node is, and a block hands
 * over the answers of its children already built instead of publishing what it keeps inside.
 *
 * @param <T> what the operation answers for one node
 */
public interface ItineraryNodeVisitor<T> {

    T activity(ItineraryItem item);

    T sequence(ItineraryNode block, List<T> parts);

    T parallel(ItineraryNode block, List<T> branches);
}
