package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.business.ports.in.CreateExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class FieldOpsApplicationTest {

    @Autowired
    private CreateExpeditionPort createExpedition;

    @Autowired
    private ExpeditionRepository repository;

    @Test
    void contextLoadsWithTheWiredBeans() {
        assertNotNull(createExpedition);
        assertNotNull(repository);
    }
}
