package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.business.fixtures.ResourceCatalogContract;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;

class InMemoryResourceCatalogTest extends ResourceCatalogContract {

    @Override
    protected ResourceCatalog createCatalog() {
        return new InMemoryResourceCatalog();
    }
}
