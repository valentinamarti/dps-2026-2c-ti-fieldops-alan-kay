package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.business.ports.in.ApproveExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

@TestConfiguration(proxyBeanMethods = false)
class DispatcherTestConfiguration {
    static final LocalDateTime NOON = LocalDateTime.of(2026, 10, 1, 12, 0);

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(NOON);
    }

    @Bean
    RecordingConsumer recordingConsumer() {
        return new RecordingConsumer();
    }

    @Bean
    FlakyConsumer flakyConsumer() {
        return new FlakyConsumer();
    }

    @Bean
    ApprovedExpeditions approvedExpeditions(ExpeditionRepository expeditions, ApproveExpeditionPort approveExpedition,
                                            ResourceCatalog catalog, ZoneRepository zones) {
        return new ApprovedExpeditions(expeditions, approveExpedition, catalog, zones);
    }
}
