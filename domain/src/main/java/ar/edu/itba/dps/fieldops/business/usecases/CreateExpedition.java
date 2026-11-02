package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.commands.CreateExpeditionCommand;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionBuilder;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.responses.ExpeditionView;
import ar.edu.itba.dps.fieldops.business.ports.in.CreateExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;

import java.util.Objects;

public class CreateExpedition implements CreateExpeditionPort {

    private final ExpeditionRepository repository;
    private final ZoneRepository zones;
    private final ResourceCatalog catalog;

    public CreateExpedition(ExpeditionRepository repository, ZoneRepository zones, ResourceCatalog catalog) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.zones = Objects.requireNonNull(zones, "zones are required");
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public ExpeditionView create(CreateExpeditionCommand command) {
        final var draft = new ExpeditionBuilder()
                .withName(command.name())
                .withPeriod(command.period())
                .withRestrictions(command.restrictions());
        command.objectives().forEach(draft::addObjective);
        command.zoneIds().forEach(id -> draft.addZone(Lookups.zone(zones, id)));
        command.responsibleIds().forEach(id -> draft.addResponsible(Lookups.staff(catalog, id)));
        command.grantedPermits().forEach(draft::addPermit);
        return ExpeditionView.of(create(draft));
    }

    Expedition create(ExpeditionBuilder draft) {
        final var expedition = draft.build(repository);
        repository.add(expedition);
        return expedition;
    }
}
