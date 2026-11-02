package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.responses.InstrumentView;
import ar.edu.itba.dps.fieldops.business.models.responses.PersonView;
import ar.edu.itba.dps.fieldops.business.models.responses.SupplyView;
import ar.edu.itba.dps.fieldops.business.models.responses.VehicleView;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryCatalogPort;

import java.util.List;
import java.util.Objects;

public class QueryCatalog implements QueryCatalogPort {
    private final ResourceCatalog catalog;

    public QueryCatalog(ResourceCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public List<PersonView> staff() {
        return catalog.allStaff().stream().map(PersonView::of).toList();
    }

    @Override
    public PersonView person(ResourceId id) {
        return PersonView.of(Lookups.staff(catalog, id));
    }

    @Override
    public List<VehicleView> vehicles() {
        return ResourceKinds.only(ResourceKinds.VEHICLES, catalog.allEquipment()).stream().map(VehicleView::of).toList();
    }

    @Override
    public VehicleView vehicle(ResourceId id) {
        return VehicleView.of(Lookups.vehicle(catalog, id));
    }

    @Override
    public List<InstrumentView> instruments() {
        return ResourceKinds.only(ResourceKinds.INSTRUMENTS, catalog.allEquipment()).stream().map(InstrumentView::of).toList();
    }

    @Override
    public InstrumentView instrument(ResourceId id) {
        return InstrumentView.of(Lookups.instrument(catalog, id));
    }

    @Override
    public List<SupplyView> supplies() {
        return ResourceKinds.only(ResourceKinds.SUPPLIES, catalog.allSupplies()).stream().map(SupplyView::of).toList();
    }

    @Override
    public SupplyView supply(ResourceId id) {
        return SupplyView.of(Lookups.depletable(catalog, id));
    }
}
