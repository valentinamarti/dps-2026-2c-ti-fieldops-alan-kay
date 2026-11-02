package ar.edu.itba.dps.fieldops.business.models.commands;

import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.util.Set;

public record RegisterPersonCommand(ResourceId id, String name, Set<Certification> certifications) {
}
