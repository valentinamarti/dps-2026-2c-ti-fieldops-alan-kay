package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.business.models.commands.RegisterZoneCommand;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryZonesPort;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterZonePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataSurvivesARestartTest {

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(FieldOpsApplication.class).properties("server.port=0").run();
    }

    @Test
    void whatWasSavedIsStillThereAfterStartingTheApplicationAgain() {
        final var id = UUID.randomUUID().toString();
        try (var first = start()) {
            first.getBean(RegisterZonePort.class).register(new RegisterZoneCommand(id, "Coastal reef", Set.of()));
        }

        try (var second = start()) {
            assertEquals("Coastal reef", second.getBean(QueryZonesPort.class).get(id).name());
        }
    }
}
