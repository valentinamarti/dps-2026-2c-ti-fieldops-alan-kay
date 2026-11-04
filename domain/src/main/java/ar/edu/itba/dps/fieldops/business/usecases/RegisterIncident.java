package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.commands.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterIncidentPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.Objects;

public class RegisterIncident implements RegisterIncidentPort {
    private final ExpeditionRepository expeditions;

    public RegisterIncident(ExpeditionRepository expeditions) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
    }

    @Override
    public Incident register(RegisterIncidentCommand command) {
        final var expedition = Lookups.expedition(expeditions, command.expeditionId());
        final var incident = expedition.registerIncident(Lookups.item(expedition, command.activityId()), command.report());
        expeditions.save(expedition);
        return incident;
    }
}
