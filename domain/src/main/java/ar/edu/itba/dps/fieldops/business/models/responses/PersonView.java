package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.resources.Certifications;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

public record PersonView(ResourceId id, String name, Certifications certifications) {

    public static PersonView of(Person person) {
        return new PersonView(person.getId(), person.getName(), person.getCertifications());
    }
}
