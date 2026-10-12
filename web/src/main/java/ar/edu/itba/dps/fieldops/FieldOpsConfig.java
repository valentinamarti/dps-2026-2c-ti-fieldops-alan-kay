package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.adapters.driven.InMemoryExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import ar.edu.itba.dps.fieldops.business.usecases.ApproveExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.CreateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.FinishExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.ReplanExpedition;
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
    ResourceReservations resourceReservations() {
        return new ResourceReservations();
    }

    @Bean
    CreateExpedition createExpedition(ExpeditionRepository repository) {
        return new CreateExpedition(repository);
    }

    @Bean
    ApproveExpedition approveExpedition(ResourceReservations reservations, ExpeditionRepository repository) {
        return new ApproveExpedition(List.of(), reservations, repository);
    }

    @Bean
    ReplanExpedition replanExpedition(ExpeditionRepository repository) {
        return new ReplanExpedition(repository, new ExpeditionReplanner(repository));
    }

    @Bean
    FinishExpedition finishExpedition(ResourceReservations reservations) {
        return new FinishExpedition(reservations);
    }

    @Bean
    ExpeditionReporter expeditionReporter() {
        return new ExpeditionReporter(new ExpeditionEstimator());
    }
}
