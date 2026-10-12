package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * The body of {@code POST /api/expeditions/{id}/blocks}, and every node inside it: a node that brings an
 * {@code activity} is a leaf, described with the same body as the schedule of a single activity, and any other
 * node is a block that says of what {@code type} it is and what its {@code children} are (D54).
 *
 * <p>A tree in one body cannot be split into a sub-resource per kind of node the way the catalog splits
 * vehicles and instruments (D52), so the kind travels as a field. Which names there are is not checked here:
 * the domain rejects one that is not a kind of block, like any other malformed plan (D49).
 *
 * @param type     {@code sequential} or {@code parallel}, only for a block
 * @param children the nodes inside a block, in the order they run when it is sequential
 * @param activity the activity of a leaf, which is what tells a leaf from a block
 */
public record ScheduleBlockRequest(String type, List<ScheduleBlockRequest> children,
                                   ScheduleActivityRequest activity) {
}
