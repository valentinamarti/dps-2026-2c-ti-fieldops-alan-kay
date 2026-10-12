package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.adapters.driven.InMemoryExpeditionRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.InMemoryZoneRepository;
import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import ar.edu.itba.dps.fieldops.business.usecases.ApproveExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.AssignResources;
import ar.edu.itba.dps.fieldops.business.usecases.CreateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.EstimateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.FindAssignmentCandidates;
import ar.edu.itba.dps.fieldops.business.usecases.FinishExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.QueryExpeditions;
import ar.edu.itba.dps.fieldops.business.usecases.ReplanExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.ReportExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleActivity;
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
    CreateExpedition createExpedition(ExpeditionRepository repository, ZoneRepository zones, ResourceCatalog catalog) {
        return new CreateExpedition(repository, zones, catalog);
    }

    @Bean
    ApproveExpedition approveExpedition(ResourceReservations reservations, ExpeditionRepository repository,
                                        ResourceCatalog catalog) {
        return new ApproveExpedition(List.of(), reservations, repository, catalog);
    }

    @Bean
    ReplanExpedition replanExpedition(ExpeditionRepository repository) {
        return new ReplanExpedition(repository, new ExpeditionReplanner(repository));
    }

    @Bean
    FinishExpedition finishExpedition(ResourceReservations reservations, ExpeditionRepository repository) {
        return new FinishExpedition(reservations, repository);
    }

    @Bean
    ScheduleActivity scheduleActivity(ExpeditionRepository repository) {
        return new ScheduleActivity(repository);
    }

    @Bean
    AssignResources assignResources(ExpeditionRepository repository, ResourceCatalog catalog) {
        return new AssignResources(repository, catalog);
    }

    @Bean
    TransitionExpedition transitionExpedition(ExpeditionRepository repository) {
        return new TransitionExpedition(repository);
    }

    @Bean
    TrackActivity trackActivity(ExpeditionRepository repository) {
        return new TrackActivity(repository);
    }

    @Bean
    QueryExpeditions queryExpeditions(ExpeditionRepository repository) {
        return new QueryExpeditions(repository);
    }

    @Bean
    ValidateExpedition validateExpedition(ExpeditionRepository repository) {
        return new ValidateExpedition(repository, List.of());
    }

    @Bean
    EstimateExpedition estimateExpedition(ExpeditionRepository repository, ExpeditionEstimator estimator) {
        return new EstimateExpedition(repository, estimator);
    }

    @Bean
    ReportExpedition reportExpedition(ExpeditionRepository repository, ExpeditionReporter reporter) {
        return new ReportExpedition(repository, reporter);
    }

    @Bean
    FindAssignmentCandidates findAssignmentCandidates(ExpeditionRepository repository, ResourceCatalog catalog) {
        return new FindAssignmentCandidates(repository, catalog);
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
