package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.util.Set;

/**
 * Everything needed to register a person in the catalog. The id comes from whoever registers the person, so
 * two people with the same id are rejected by the catalog and not renamed.
 */
public record RegisterPersonCommand(ResourceId id, String name, Set<Certification> certifications) {
}
