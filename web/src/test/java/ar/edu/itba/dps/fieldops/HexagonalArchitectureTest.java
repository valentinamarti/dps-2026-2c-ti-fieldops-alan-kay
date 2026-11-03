package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTest {
    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("ar.edu.itba.dps.fieldops");

    @Test
    void theDrivingAdaptersOnlyKnowTheInputPortsAndNotTheirImplementations() {
        noClasses().that().resideInAPackage("..adapters.driving..")
                .should().dependOnClassesThat().resideInAPackage("..business.usecases..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void theDrivingAdaptersDoNotKnowTheMutableEntitiesOfTheDomain() {
        noClasses().that().resideInAPackage("..adapters.driving..")
                .should().dependOnClassesThat().belongToAnyOf(Expedition.class, ItineraryItem.class, ReplanProposal.class,
                        Activity.class, Person.class, Vehicle.class, Instrument.class, Depletable.class, Zone.class)
                .check(PRODUCTION_CLASSES);
    }
}
