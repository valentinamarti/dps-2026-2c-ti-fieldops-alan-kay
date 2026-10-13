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

/**
 * Turns {@link DemoDataLoader} on with the {@code demo} profile: the only piece of the seed that knows about
 * Spring, so the loader itself stays framework-free and testable without a context (D37, D50).
 *
 * <p>The documents come in as classpath resources, so the same build serves {@code mvn spring-boot:run}, the
 * packaged jar and the tests, with no working directory to get right. Where they are is a property, so another
 * scenario is another pair of files and not another class.
 */
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

    /**
     * The seed runs while the context is being built, before the web server starts taking requests, so the
     * first request of the script already finds the scenario loaded. A document that cannot be read or that
     * the domain rejects fails the startup, which is what should happen: a half-loaded demo is worse than one
     * that does not start.
     */
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

    /**
     * What the seed left loaded. It is a bean so that loading happens once, when the context is built, and so
     * that whoever needs to address the demonstration expedition has where to read its id from; over HTTP it
     * is {@code GET /api/expeditions}.
     */
    public record DemoData(List<ExpeditionId> expeditionIds) {
    }
}
