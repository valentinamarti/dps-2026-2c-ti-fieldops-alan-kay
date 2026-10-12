package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.business.fixtures.ReplanProposalRepositoryContract;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;

class InMemoryReplanProposalRepositoryTest extends ReplanProposalRepositoryContract {

    @Override
    protected ReplanProposalRepository createRepository() {
        return new InMemoryReplanProposalRepository();
    }
}
