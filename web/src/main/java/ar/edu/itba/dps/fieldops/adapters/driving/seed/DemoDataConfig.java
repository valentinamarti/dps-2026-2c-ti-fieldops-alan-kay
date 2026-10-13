package ar.edu.itba.dps.fieldops.adapters.driving.seed;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.usecases.AssignResources;
import ar.edu.itba.dps.fieldops.business.usecases.CreateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.MarkResourceUnavailable;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterResources;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterZone;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleActivity;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Configuration
@Profile("demo")
public class DemoDataConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DemoDataConfig.class);

    @Bean
    DemoDataLoader demoDataLoader(RegisterZone registerZone, RegisterResources registerResources,
                                  MarkResourceUnavailable markResourceUnavailable, CreateExpedition createExpedition,
                                  ScheduleActivity scheduleActivity, ScheduleBlock scheduleBlock,
                                  AssignResources assignResources, ObjectMapper json) {
        return new DemoDataLoader(registerZone, registerResources, markResourceUnavailable, createExpedition,
                scheduleActivity, scheduleBlock, assignResources, json);
    }

    @Bean
    DemoData demoData(DemoDataLoader loader,
                      @Value("${fieldops.demo.catalog:classpath:demo/catalog.json}") Resource catalog,
                      @Value("${fieldops.demo.expeditions:classpath:demo/expedition.json}") Resource expeditions)
            throws IOException {
        final var loaded = new DemoData(loader.load(catalog.getInputStream(), expeditions.getInputStream()));
        LOG.info("Demo profile: loaded {} and {}, with the expeditions {}", catalog.getFilename(),
                expeditions.getFilename(), loaded.expeditionIds());
        return loaded;
    }

    public record DemoData(List<ExpeditionId> expeditionIds) {
    }
}
