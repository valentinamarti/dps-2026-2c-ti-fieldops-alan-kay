package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "replan_proposal")
@Getter
@Setter
public class ReplanProposalEntity {
    @Id
    private String id;

    @Column(name = "expedition_id")
    private String expeditionId;

    private String status;

    @Embedded
    private IncidentColumns incident;

    @Column(name = "responsible_id")
    private String responsibleId;

    private String justification;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "chosen_option")
    private Integer chosenOption;

    @Column(name = "alternative_expedition")
    private String alternative;

    @Column(name = "superseded_by")
    private String supersededBy;

    @Version
    private Long version;

    @ElementCollection
    @CollectionTable(name = "replan_option", joinColumns = @JoinColumn(name = "proposal_id"))
    @OrderColumn(name = "sort_order")
    private List<ReplanOptionRow> options = new ArrayList<>();

    @Column(name = "created_order", insertable = false, updatable = false)
    private Long createdOrder;
}
