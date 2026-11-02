package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.adapters.driven.InMemoryExpeditionRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.InMemoryZoneRepository;
import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.ports.in.ApproveExpeditionPort;
import ar.edu.itba.dps.fieldops.business.ports.in.AssignResourcesPort;
import ar.edu.itba.dps.fieldops.business.ports.in.CreateExpeditionPort;
import ar.edu.itba.dps.fieldops.business.ports.in.EstimateExpeditionPort;
import ar.edu.itba.dps.fieldops.business.ports.in.FindAssignmentCandidatesPort;
import ar.edu.itba.dps.fieldops.business.ports.in.FinishExpeditionPort;
import ar.edu.itba.dps.fieldops.business.ports.in.MarkResourceUnavailablePort;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryActivityTypesPort;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryCatalogPort;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryExpeditionsPort;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryZonesPort;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterResourcesPort;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterZonePort;
import ar.edu.itba.dps.fieldops.business.ports.in.ReportExpeditionPort;
import ar.edu.itba.dps.fieldops.business.ports.in.ScheduleActivityPort;
import ar.edu.itba.dps.fieldops.business.ports.in.ScheduleBlockPort;
import ar.edu.itba.dps.fieldops.business.ports.in.TrackActivityPort;
import ar.edu.itba.dps.fieldops.business.ports.in.TransitionExpeditionPort;
import ar.edu.itba.dps.fieldops.business.ports.in.ValidateExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionIdProvider;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import ar.edu.itba.dps.fieldops.business.usecases.ApproveExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.AssignResources;
import ar.edu.itba.dps.fieldops.business.usecases.CreateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.EstimateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.FindAssignmentCandidates;
import ar.edu.itba.dps.fieldops.business.usecases.FinishExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.MarkResourceUnavailable;
import ar.edu.itba.dps.fieldops.business.usecases.QueryActivityTypes;
import ar.edu.itba.dps.fieldops.business.usecases.QueryCatalog;
import ar.edu.itba.dps.fieldops.business.usecases.QueryExpeditions;
import ar.edu.itba.dps.fieldops.business.usecases.QueryZones;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterResources;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterZone;
import ar.edu.itba.dps.fieldops.business.usecases.ReplanExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.ReportExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleActivity;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleBlock;
import ar.edu.itba.dps.fieldops.business.usecases.TrackActivity;
import ar.edu.itba.dps.fieldops.business.usecases.TransitionExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.ValidateExpedition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class FieldOpsConfig {

    @Bean
    ExpeditionRepository expeditionRepository() {
        return new InMemoryExpeditionRepository();
    }

    @Bean
    ZoneRepository zoneRepository() {
        return new InMemoryZoneRepository();
    }

    @Bean
    ResourceCatalog resourceCatalog() {
        return new ResourceCatalog();
    }

    @Bean
    ResourceReservations resourceReservations() {
        return new ResourceReservations();
    }

    @Bean
    CreateExpeditionPort createExpedition(ExpeditionRepository repository, ZoneRepository zones, ResourceCatalog catalog) {
        return new CreateExpedition(repository, zones, catalog);
    }

    @Bean
    ApproveExpeditionPort approveExpedition(ResourceReservations reservations, ExpeditionRepository repository,
                                        ResourceCatalog catalog) {
        return new ApproveExpedition(List.of(), reservations, repository, catalog);
    }

    @Bean
    ExpeditionReplanner expeditionReplanner(ExpeditionIdProvider ids) {
        return new ExpeditionReplanner(ids);
    }

    @Bean
    ReplanExpedition replanExpedition(ExpeditionRepository repository, ExpeditionReplanner replanner) {
        return new ReplanExpedition(repository, replanner);
    }

    @Bean
    FinishExpeditionPort finishExpedition(ResourceReservations reservations, ExpeditionRepository repository) {
        return new FinishExpedition(reservations, repository);
    }

    @Bean
    ScheduleActivityPort scheduleActivity(ExpeditionRepository repository, ZoneRepository zones,
                                      ActivityTypeRegistry types) {
        return new ScheduleActivity(repository, zones, types);
    }

    @Bean
    ScheduleBlockPort scheduleBlock(ExpeditionRepository repository, ZoneRepository zones, ActivityTypeRegistry types) {
        return new ScheduleBlock(repository, zones, types);
    }

    @Bean
    AssignResourcesPort assignResources(ExpeditionRepository repository, ResourceCatalog catalog) {
        return new AssignResources(repository, catalog);
    }

    @Bean
    TransitionExpeditionPort transitionExpedition(ExpeditionRepository repository) {
        return new TransitionExpedition(repository);
    }

    @Bean
    TrackActivityPort trackActivity(ExpeditionRepository repository) {
        return new TrackActivity(repository);
    }

    @Bean
    QueryExpeditionsPort queryExpeditions(ExpeditionRepository repository) {
        return new QueryExpeditions(repository);
    }

    @Bean
    RegisterResourcesPort registerResources(ResourceCatalog catalog) {
        return new RegisterResources(catalog);
    }

    @Bean
    MarkResourceUnavailablePort markResourceUnavailable(ResourceCatalog catalog) {
        return new MarkResourceUnavailable(catalog);
    }

    @Bean
    QueryCatalogPort queryCatalog(ResourceCatalog catalog) {
        return new QueryCatalog(catalog);
    }

    @Bean
    RegisterZonePort registerZone(ZoneRepository zones) {
        return new RegisterZone(zones);
    }

    @Bean
    QueryZonesPort queryZones(ZoneRepository zones) {
        return new QueryZones(zones);
    }

    @Bean
    ValidateExpeditionPort validateExpedition(ExpeditionRepository repository) {
        return new ValidateExpedition(repository, List.of());
    }

    @Bean
    EstimateExpeditionPort estimateExpedition(ExpeditionRepository repository, ExpeditionEstimator estimator) {
        return new EstimateExpedition(repository, estimator);
    }

    @Bean
    ReportExpeditionPort reportExpedition(ExpeditionRepository repository, ExpeditionReporter reporter) {
        return new ReportExpedition(repository, reporter);
    }

    @Bean
    FindAssignmentCandidatesPort findAssignmentCandidates(ExpeditionRepository repository, ResourceCatalog catalog) {
        return new FindAssignmentCandidates(repository, catalog);
    }

    @Bean
    ActivityTypeRegistry activityTypeRegistry() {
        return ActivityTypeRegistry.withStandardTypes();
    }

    @Bean
    QueryActivityTypesPort queryActivityTypes(ActivityTypeRegistry registry) {
        return new QueryActivityTypes(registry);
    }

    @Bean
    ExpeditionEstimator expeditionEstimator() {
        return new ExpeditionEstimator();
    }

    @Bean
    ExpeditionReporter expeditionReporter(ExpeditionEstimator estimator) {
        return new ExpeditionReporter(estimator);
    }
}
