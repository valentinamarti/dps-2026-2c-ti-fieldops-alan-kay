package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.util.Set;

public record RegisterInstrumentCommand(ResourceId id, String name, ResourceCategory category,
                                        Set<Certification> certifications) {
}
