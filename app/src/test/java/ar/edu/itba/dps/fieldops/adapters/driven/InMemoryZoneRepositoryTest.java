package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.business.fixtures.ZoneRepositoryContract;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;

class InMemoryZoneRepositoryTest extends ZoneRepositoryContract {

    @Override
    protected ZoneRepository createRepository() {
        return new InMemoryZoneRepository();
    }
}
