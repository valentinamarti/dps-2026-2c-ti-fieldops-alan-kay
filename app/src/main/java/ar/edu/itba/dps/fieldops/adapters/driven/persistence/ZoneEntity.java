package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "zone")
@Getter
@Setter
public class ZoneEntity {
    @Id
    private String id;

    private String name;

    @ElementCollection
    @CollectionTable(name = "zone_permit", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "permit")
    private Set<String> permits = new HashSet<>();

    @Column(name = "created_order", insertable = false, updatable = false)
    private Long createdOrder;
}
