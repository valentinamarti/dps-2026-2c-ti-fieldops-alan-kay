package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.fixtures.ResourceCatalogContract;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JpaResourceCatalogTest extends ResourceCatalogContract {
    @Autowired
    private ResourceCatalog resources;
    @Autowired
    private DatabaseCleaner cleaner;

    @Override
    protected ResourceCatalog createCatalog() {
        cleaner.clean();
        return resources;
    }
}
