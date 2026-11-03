package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionRepositoryContract;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JpaExpeditionRepositoryTest extends ExpeditionRepositoryContract {
    @Autowired
    private ExpeditionRepository expeditions;
    @Autowired
    private ZoneRepository zones;
    @Autowired
    private ResourceCatalog catalog;
    @Autowired
    private DatabaseCleaner cleaner;

    @Override
    protected ExpeditionRepository createRepository() {
        cleaner.clean();
        return expeditions;
    }

    @Override
    protected void registerReferences(Zone zone, Person responsible) {
        zones.add(zone);
        catalog.addStaff(responsible);
    }
}
