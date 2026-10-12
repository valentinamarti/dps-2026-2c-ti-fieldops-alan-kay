package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionRepositoryContract;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

class InMemoryExpeditionRepositoryTest extends ExpeditionRepositoryContract {

    @Override
    protected ExpeditionRepository createRepository() {
        return new InMemoryExpeditionRepository();
    }
}
