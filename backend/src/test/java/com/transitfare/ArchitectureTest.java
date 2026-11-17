package com.transitfare;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;

@AnalyzeClasses(packages = "com.transitfare", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final List<String> MODULES = List.of("transit", "pass", "itinerary", "directions");

    @ArchTest
    @DisplayName("다른 모듈은 그 모듈의 퍼사드와 contract 패키지만 사용한다")
    void modulesOnlyUseOtherModulesFacadeAndContract(JavaClasses classes) {
        for (String module : MODULES) {
            String base = "com.transitfare." + module;
            String[] others = MODULES.stream().filter(m -> !m.equals(module))
                    .map(m -> "com.transitfare." + m + "..").toArray(String[]::new);

            noClasses().that().resideInAnyPackage(others)
                    .should().dependOnClassesThat(
                            resideInAPackage(base + "..")
                                    .and(not(resideInAPackage(base + ".contract..")))
                                    .and(not(resideInAPackage(base).and(simpleNameEndingWith("Facade")))))
                    .allowEmptyShould(true)
                    .check(classes);
        }
    }

    @ArchTest
    @DisplayName("pass는 transit을 전혀 모른다")
    void passDoesNotDependOnTransit(JavaClasses classes) {
        noClasses().that().resideInAPackage("com.transitfare.pass..")
                .should().dependOnClassesThat().resideInAPackage("com.transitfare.transit..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @ArchTest
    @DisplayName("domain 패키지는 Spring과 JPA에 의존하지 않는다")
    void domainHasNoSpringDependency(JavaClasses classes) {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @ArchTest
    @DisplayName("common은 기능 모듈에 의존하지 않는다")
    void commonDoesNotDependOnModules(JavaClasses classes) {
        noClasses().that().resideInAPackage("com.transitfare.common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.transitfare.transit..", "com.transitfare.pass..",
                        "com.transitfare.itinerary..", "com.transitfare.directions..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
