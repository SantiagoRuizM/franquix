package com.franquix.franchises.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Guards the dependency rules of the hexagonal architecture. */
@AnalyzeClasses(packages = "com.franquix.franchises", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "com.franquix.franchises.domain..";
    private static final String APPLICATION = "com.franquix.franchises.application..";
    private static final String INFRASTRUCTURE = "com.franquix.franchises.infrastructure..";

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "org.bson..", "com.mongodb..", "jakarta..")
            .because("the domain must not depend on any framework");

    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, INFRASTRUCTURE);

    @ArchTest
    static final ArchRule applicationIsFrameworkFree = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "org.bson..", "com.mongodb..", INFRASTRUCTURE)
            .because("use cases only orchestrate domain ports; Spring wiring lives in infrastructure");
}
