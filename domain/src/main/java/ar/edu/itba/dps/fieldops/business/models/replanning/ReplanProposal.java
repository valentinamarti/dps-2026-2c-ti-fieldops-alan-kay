package ar.edu.itba.dps.fieldops.business.models.replanning;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.exceptions.ProposalAlreadyDecidedException;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ReplanProposal {
    @Getter
    private final ReplanProposalId id;
    @Getter
    private final ExpeditionId originalExpedition;
    @Getter
    private final Incident incident;
    @Getter
    private final List<ReplanOption> options;
    @Getter
    private ReplanProposalStatus status = ReplanProposalStatus.PENDING;
    private ReplanDecision decision;
    private Integer chosenOption;
    private ExpeditionId alternative;
    private ReplanProposalId supersededBy;

    public ReplanProposal(ReplanProposalId id, ExpeditionId originalExpedition, Incident incident, List<ReplanOption> options) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.originalExpedition = Objects.requireNonNull(originalExpedition, "originalExpedition is required");
        this.incident = Objects.requireNonNull(incident, "incident is required");
        this.options = DomainArguments.requireList(options, "options");
    }

    public ActivityId getAffectedActivity() {
        return incident.activity();
    }

    public ReplanOption option(int number) {
        if (number < 1 || number > options.size()) {
            throw new NotFoundException("replan option", "%d of the proposal %s".formatted(number, id));
        }
        return options.get(number - 1);
    }

    public boolean isPending() {
        return status == ReplanProposalStatus.PENDING;
    }

    public void requirePending() {
        if (!isPending()) {
            throw new ProposalAlreadyDecidedException(id, status);
        }
    }

    public void accept(int optionNumber, ReplanDecision decision, ExpeditionId alternative) {
        requirePending();
        option(optionNumber);
        this.decision = Objects.requireNonNull(decision, "decision is required");
        this.alternative = Objects.requireNonNull(alternative, "alternative is required");
        this.chosenOption = optionNumber;
        status = ReplanProposalStatus.ACCEPTED;
    }

    public void reject(ReplanDecision decision) {
        requirePending();
        this.decision = Objects.requireNonNull(decision, "decision is required");
        status = ReplanProposalStatus.REJECTED;
    }

    public void supersedeBy(ReplanProposalId accepted) {
        requirePending();
        this.supersededBy = Objects.requireNonNull(accepted, "accepted is required");
        status = ReplanProposalStatus.SUPERSEDED;
    }

    public Optional<ReplanDecision> getDecision() {
        return Optional.ofNullable(decision);
    }

    public Optional<ReplanOption> getChosenOption() {
        return Optional.ofNullable(chosenOption).map(this::option);
    }

    public Optional<Integer> getChosenOptionNumber() {
        return Optional.ofNullable(chosenOption);
    }

    public Optional<ExpeditionId> getAlternative() {
        return Optional.ofNullable(alternative);
    }

    public Optional<ReplanProposalId> getSupersededBy() {
        return Optional.ofNullable(supersededBy);
    }
}
