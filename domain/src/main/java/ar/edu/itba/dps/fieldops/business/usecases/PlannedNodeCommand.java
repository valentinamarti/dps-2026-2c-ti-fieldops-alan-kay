package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A node of the tree a block is asked for: an activity described by ids, type and parameters, or a block whose
 * children run in sequence or in parallel (D54).
 *
 * <p>It is to {@link ScheduleBlockCommand} what {@link PlannedNode} is to an expedition, and it is built the
 * same way (D39): every factory keeps the plan it assembles, so walking the tree never asks what kind of node
 * it is on. The names of the kinds of block live here because they are part of the vocabulary of the command:
 * an adapter sends the name it received and the domain is the one that says whether it is one of them, like
 * the registry does with the name of a type of activity (D44, D53).
 */
public final class PlannedNodeCommand {

    public static final String SEQUENTIAL = "sequential";
    public static final String PARALLEL = "parallel";

    private static final Map<String, Function<List<PlannedNode>, PlannedNode>> BLOCKS = Map.of(
            SEQUENTIAL, PlannedNode::sequential,
            PARALLEL, PlannedNode::parallel);

    private final Function<PlannedActivities, PlannedNode> planning;

    private PlannedNodeCommand(Function<PlannedActivities, PlannedNode> planning) {
        this.planning = planning;
    }

    public static PlannedNodeCommand activity(PlannedActivityCommand activity) {
        Objects.requireNonNull(activity, "activity is required");
        return new PlannedNodeCommand(activities ->
                PlannedNode.activity(activities.of(activity), activity.scheduledPeriod()));
    }

    public static PlannedNodeCommand sequential(PlannedNodeCommand... children) {
        return sequential(List.of(children));
    }

    public static PlannedNodeCommand sequential(List<PlannedNodeCommand> children) {
        return block(SEQUENTIAL, children);
    }

    public static PlannedNodeCommand parallel(PlannedNodeCommand... branches) {
        return parallel(List.of(branches));
    }

    public static PlannedNodeCommand parallel(List<PlannedNodeCommand> branches) {
        return block(PARALLEL, branches);
    }

    /**
     * The block of the kind that goes by that name. A name that is not one of them is a malformed plan, like
     * the ones D40 rejects, and the message says which names there are.
     */
    public static PlannedNodeCommand block(String kind, List<PlannedNodeCommand> children) {
        final var plans = DomainArguments.requireList(children, "children");
        final var assembler = assemblerOf(kind);
        return new PlannedNodeCommand(activities ->
                assembler.apply(plans.stream().map(plan -> plan.plan(activities)).toList()));
    }

    /**
     * The plan this node describes, building the activity of each leaf in the order they were declared, which
     * is what lets a leaf depend on an earlier one of the same tree.
     */
    PlannedNode plan(PlannedActivities activities) {
        return planning.apply(activities);
    }

    private static Function<List<PlannedNode>, PlannedNode> assemblerOf(String kind) {
        final var name = DomainArguments.requireText(kind, "kind of block").strip().toLowerCase(Locale.ROOT);
        final var assembler = BLOCKS.get(name);
        if (assembler == null) {
            throw new InvalidScheduleException("there is no kind of block named %s: a block is %s or %s"
                    .formatted(name, SEQUENTIAL, PARALLEL));
        }
        return assembler;
    }
}
