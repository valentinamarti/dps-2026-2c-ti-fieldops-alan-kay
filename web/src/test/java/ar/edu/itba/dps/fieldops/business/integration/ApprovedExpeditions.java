package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.ports.in.ApproveExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.APPROVED_AT;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.instrument;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static ar.edu.itba.dps.fieldops.business.models.common.Quantity.of;

final class ApprovedExpeditions {
    private final ExpeditionRepository expeditions;
    private final ApproveExpeditionPort approveExpedition;
    private final ResourceCatalog catalog;
    private final ZoneRepository zones;

    ApprovedExpeditions(ExpeditionRepository expeditions, ApproveExpeditionPort approveExpedition,
                        ResourceCatalog catalog, ZoneRepository zones) {
        this.expeditions = expeditions;
        this.approveExpedition = approveExpedition;
        this.catalog = catalog;
        this.zones = zones;
    }

    ExpeditionId approveOne() {
        final var coast = zone("z-1");
        final var leader = person("p-1");
        final var sampler = person("p-2");
        final var cooler = instrument("c-1", "Cooler");
        final var jars = supply("j-1", "Sample container", of("10", UNITS));
        zones.add(coast);
        List.of(leader, sampler).forEach(catalog::addStaff);
        catalog.addEquipment(cooler);
        catalog.addSupply(jars);
        final var expedition = expeditionIn(coast, leader).build(expeditions);
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(13, 15));
        expedition.assignStaff(item, sampler);
        expedition.assignEquipment(item, cooler);
        expedition.assignSupply(item, jars);
        expedition.submitForReview();
        expeditions.add(expedition);
        approveExpedition.approve(expedition.getId(), List.of(), APPROVED_AT);
        return expedition.getId();
    }
}
