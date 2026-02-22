package com.ticketrush.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.ticketrush", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyArchTest {

    @ArchTest
    static final ArchRule entities_should_not_depend_on_api_layer =
            noClasses()
                    .that().areAnnotatedWith(Entity.class)
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.api..");

    @ArchTest
    static final ArchRule controllers_should_not_depend_on_repository_directly =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.api.controller..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");
}
