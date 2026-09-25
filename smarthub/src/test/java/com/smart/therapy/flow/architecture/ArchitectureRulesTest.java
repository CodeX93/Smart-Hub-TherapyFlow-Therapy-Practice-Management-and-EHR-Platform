package com.smart.therapy.flow.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.smart.therapy.flow")
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule targeted_controllers_should_not_depend_on_repositories =
            noClasses()
                    .that().haveSimpleName("SuperAdminController")
                    .or().haveSimpleName("AdminOrganizationController")
                    .or().haveSimpleName("StripeController")
                    .or().haveSimpleName("PatientConsentController")
                    .or().haveSimpleName("ClientFilterController")
                    .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule targeted_services_should_not_directly_use_entity_manager =
            noClasses()
                    .that().haveSimpleName("SuperAdminOrganisationListService")
                    .or().haveSimpleName("StripeService")
                    .or().haveSimpleName("StripePaymentApplicationService")
                    .or().haveSimpleName("StripeWebhookApplicationService")
                    .or().haveSimpleName("PatientConsentQueryService")
                    .or().haveSimpleName("ClientFilterQueryService")
                    .or().haveSimpleName("AdminOrganizationQueryService")
                    .should().dependOnClassesThat().haveFullyQualifiedName("jakarta.persistence.EntityManager");
}
