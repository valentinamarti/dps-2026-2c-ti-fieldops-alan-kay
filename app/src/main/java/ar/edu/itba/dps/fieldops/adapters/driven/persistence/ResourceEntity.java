package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "resource")
@Getter
@Setter
public class ResourceEntity {
    @Id
    private String id;

    private String kind;

    private String name;

    private String category;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "stock_amount", precision = 19, scale = 4)
    private BigDecimal stockAmount;

    @Column(name = "stock_unit")
    private String stockUnit;

    @Version
    private Long version;

    @ElementCollection
    @CollectionTable(name = "resource_certification", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "certification")
    private Set<String> certifications = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "resource_reservation", joinColumns = @JoinColumn(name = "resource_id"))
    @OrderColumn(name = "sort_order")
    private List<ReservationRow> reservations = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "resource_unavailability", joinColumns = @JoinColumn(name = "resource_id"))
    @OrderColumn(name = "sort_order")
    private List<UnavailabilityRow> unavailabilities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "supply_reservation", joinColumns = @JoinColumn(name = "resource_id"))
    @OrderColumn(name = "sort_order")
    private List<SupplyReservationRow> supplyReservations = new ArrayList<>();

    @Column(name = "created_order", insertable = false, updatable = false)
    private Long createdOrder;
}
