package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

@SpringBootApplication
class PersistenceTestApplication {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }

    @Bean
    LoadedAggregates loadedAggregates() {
        return new LoadedAggregates();
    }

    @Bean
    ZoneRepository zoneRepository(ZoneJpaRepository zones) {
        return new JpaZoneRepository(zones);
    }

    @Bean
    ResourceCatalog resourceCatalog(ResourceJpaRepository resources, LoadedAggregates loaded) {
        return new JpaResourceCatalog(resources, loaded);
    }

    @Bean
    ExpeditionRepository expeditionRepository(ExpeditionJpaRepository expeditions, ZoneRepository zones,
                                              ResourceCatalog catalog, LoadedAggregates loaded) {
        return new JpaExpeditionRepository(expeditions, zones, catalog, ActivityTypeRegistry.withStandardTypes(), loaded);
    }

    @Bean
    ReplanProposalRepository replanProposalRepository(ReplanProposalJpaRepository proposals, ResourceCatalog catalog,
                                                      LoadedAggregates loaded) {
        return new JpaReplanProposalRepository(proposals, catalog, loaded);
    }

    @Bean
    EventLog eventLog(DomainEventJpaRepository events, EventDeliveryJpaRepository deliveries) {
        return new JpaEventLog(events, deliveries);
    }

    @Bean
    DatabaseCleaner databaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource);
    }
}
