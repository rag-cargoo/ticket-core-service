package com.ticketrush.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.ticketrush", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyArchTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_api_layer =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.api..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_api_layer =
            noClasses()
                    .that().resideInAPackage("com.ticketrush.application..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.ticketrush.api..");

    @ArchTest
    static final ArchRule rest_controllers_should_not_depend_on_repository_directly =
            noClasses()
                    .that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");
}
