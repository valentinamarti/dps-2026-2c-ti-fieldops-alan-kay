package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.fixtures.ReplanProposalRepositoryContract;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JpaReplanProposalRepositoryTest extends ReplanProposalRepositoryContract {
    @Autowired
    private ReplanProposalRepository proposals;
    @Autowired
    private ResourceCatalog catalog;
    @Autowired
    private DatabaseCleaner cleaner;

    @Override
    protected ReplanProposalRepository createRepository() {
        cleaner.clean();
        return proposals;
    }

    @Override
    protected void registerResponsible(Person responsible) {
        catalog.addStaff(responsible);
    }
}
