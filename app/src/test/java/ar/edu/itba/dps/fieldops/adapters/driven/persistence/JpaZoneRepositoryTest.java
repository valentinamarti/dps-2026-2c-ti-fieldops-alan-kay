package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.fixtures.ZoneRepositoryContract;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JpaZoneRepositoryTest extends ZoneRepositoryContract {
    @Autowired
    private ZoneRepository zones;
    @Autowired
    private DatabaseCleaner cleaner;

    @Override
    protected ZoneRepository createRepository() {
        cleaner.clean();
        return zones;
    }
}
