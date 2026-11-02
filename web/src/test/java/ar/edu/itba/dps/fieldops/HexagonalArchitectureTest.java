package ar.edu.itba.dps.fieldops;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTest {
    private static final String APPLICATION = "ar.edu.itba.dps.fieldops";

    @Test
    void theDrivingAdaptersOnlyKnowTheInputPortsAndNotTheirImplementations() {
        noClasses().that().resideInAPackage("..adapters.driving..")
                .should().dependOnClassesThat().resideInAPackage("..business.usecases..")
                .check(new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests()).importPackages(APPLICATION));
    }
}
