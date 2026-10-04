package com.example.adminbff.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.adminbff", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule coreShouldBePure = noClasses()
            .that().resideInAPackage("..core..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "com.example.adminbff.inbound..",
                    "com.example.adminbff.outbound.."
            );

    @ArchTest
    static final ArchRule inboundShouldDependOnCore = classes()
            .that().resideInAPackage("..inbound..")
            .should().dependOnClassesThat().resideInAPackage("..core..");

    @ArchTest
    static final ArchRule outboundShouldDependOnCore = classes()
            .that().resideInAPackage("..outbound..")
            .should().dependOnClassesThat().resideInAPackage("..core..");

    @ArchTest
    static final ArchRule inboundShouldNotDependOnOutbound = noClasses()
            .that().resideInAPackage("..inbound..")
            .should().dependOnClassesThat().resideInAPackage("..outbound..");

    @ArchTest
    static final ArchRule outboundShouldNotDependOnInbound = noClasses()
            .that().resideInAPackage("..outbound..")
            .should().dependOnClassesThat().resideInAPackage("..inbound..");
}
