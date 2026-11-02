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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "expedition")
@Getter
@Setter
public class ExpeditionEntity {
    @Id
    private String id;

    private String name;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "max_participants")
    private int maxParticipants;

    @Column(name = "risk_tolerance")
    private String riskTolerance;

    private String status;

    @Column(name = "replaced_expedition")
    private String replacedExpedition;

    @Column(name = "replaced_revision")
    private Integer replacedRevision;

    @Embedded
    private ReasonColumns reason;

    @Column(name = "replacement_expedition")
    private String replacement;

    @Column(name = "execution_revision")
    private int executionRevision;

    @Version
    private Long version;

    @ElementCollection
    @CollectionTable(name = "expedition_objective", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "objective")
    private List<String> objectives = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "expedition_zone", joinColumns = @JoinColumn(name = "expedition_id"))
    @Column(name = "zone_id")
    private Set<String> zoneIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "expedition_responsible", joinColumns = @JoinColumn(name = "expedition_id"))
    @Column(name = "person_id")
    private Set<String> responsibleIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "expedition_permit", joinColumns = @JoinColumn(name = "expedition_id"))
    @Column(name = "permit")
    private Set<String> permits = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "accepted_warning", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "sort_order")
    private List<AcceptedWarningRow> acceptedWarnings = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "expedition_incident", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "sort_order")
    private List<IncidentColumns> incidents = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "expedition_activity", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "sort_order")
    private List<ActivityRow> activities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "activity_parameter", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "sort_order")
    private List<ActivityParameterRow> parameters = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "activity_dependency", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "sort_order")
    private List<ActivityDependencyRow> dependencies = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "itinerary_node", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "sort_order")
    private List<ItineraryNodeRow> nodes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "node_assignment", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "sort_order")
    private List<NodeAssignmentRow> assignments = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "node_observation", joinColumns = @JoinColumn(name = "expedition_id"))
    @OrderColumn(name = "sort_order")
    private List<NodeObservationRow> observations = new ArrayList<>();

    @Column(name = "created_order", insertable = false, updatable = false)
    private Long createdOrder;
}
