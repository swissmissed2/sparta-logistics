package com.sparta.logistics.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

public class CommonArchRules {

    /**
     * 레이어드 아키텍처 규칙
     * controller -> service -> (client/repository/entity) 방향만 허용합니다.
     */
    public static void layerDependencyRule(JavaClasses classes, String basePackage) {

        // client 패키지 존재 여부 확인
        boolean hasClient = classes.stream()
                .anyMatch(c -> c.getPackageName().contains(basePackage + ".client"));

        // repository 패키지 존재 여부 확인
        boolean hasRepository = classes.stream()
                .anyMatch(c -> c.getPackageName().contains(basePackage + ".repository"));

        // entity 패키지 존재 여부 확인
        boolean hasEntity = classes.stream()
                .anyMatch(c -> c.getPackageName().contains(basePackage + ".entity"));

        // dto 패키지 존재 여부 확인
        boolean hasRequestDto = classes.stream()
                .anyMatch(c -> c.getPackageName().contains(basePackage + ".dto.request"));

        boolean hasResponseDto = classes.stream()
                .anyMatch(c -> c.getPackageName().contains(basePackage + ".dto.response"));

        LayeredArchitecture rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controller").definedBy(basePackage + "..controller..")
                .layer("Service").definedBy(basePackage + "..service..")
                .layer("DTO").definedBy(basePackage + "..dto..")
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("DTO").mayOnlyBeAccessedByLayers("Controller", "Service");

        // client 패키지가 있을 때만 레이어 추가
        if (hasClient) {
            rule = rule
                    .layer("Client").definedBy(basePackage + "..client..")
                    .whereLayer("Client").mayOnlyBeAccessedByLayers("Service");
        }

        // repository 패키지가 있을 때만 레이어 추가
        if (hasRepository) {
            rule = rule
                    .layer("Repository").definedBy(basePackage + "..repository..")
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");
        }

        // entity 패키지가 있을 때만 레이어 추가
        if (hasEntity) {
            rule = rule
                    .layer("Entity").definedBy(basePackage + "..entity..")
                    .whereLayer("Entity").mayOnlyBeAccessedByLayers("Service", "Repository");
        }

        // RequestDto 패키지가 있을 때만 레이어 추가
        if (hasRequestDto) {
            rule = rule
                    .layer("DTO-Request").definedBy(basePackage + "..dto..request..")
                    .whereLayer("DTO-Request").mayOnlyBeAccessedByLayers("Controller", "Service");
        }

        // ResponseDto 패키지가 있을 때만 레이어 추가
        if (hasResponseDto) {
            rule = rule
                    .layer("DTO-Response").definedBy(basePackage + "..dto..response..")
                    .whereLayer("DTO-Response").mayOnlyBeAccessedByLayers("Controller", "Service");
        }

        rule.check(classes);
    }

    /**
     * Controller 네이밍 규칙: 클래스명이 Controller로 끝나야 합니다.
     */
    public static ArchRule controllerNamingRule(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..controller..")
                .should().haveSimpleNameEndingWith("Controller")
                .as("Controller 패키지의 클래스는 'Controller'로 끝나야 합니다.");
    }

    /**
     * Service 네이밍 규칙: 클래스명이 Service로 끝나야 합니다.
     */
    public static ArchRule serviceNamingRule(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..service..")
                .should().haveSimpleNameEndingWith("Service")
                .as("Service 패키지의 클래스는 'Service'로 끝나야 합니다.");
    }

    /**
     * Repository 네이밍 규칙: 클래스명이 Repository로 끝나야 합니다.
     */
    public static ArchRule repositoryNamingRule(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..repository..")
                .should().haveSimpleNameEndingWith("Repository")
                .as("Repository 패키지의 클래스는 'Repository'로 끝나야 합니다.");
    }

    /**
     * Entity 네이밍 규칙: 클래스명이 Entity로 끝나야 합니다.
     */
    public static ArchRule entityNamingRule(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..entity..")
                .and().haveSimpleNameNotContaining("$")
                .should().haveSimpleNameEndingWith("Entity")
                .as("Entity 패키지의 클래스는 'Entity'로 끝나야 합니다.");
    }

    /**
     * Request DTO 네이밍 규칙: 클래스명이 Request로 끝나야 합니다.
     */
    public static ArchRule requestDtoNamingRule(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..dto..request..")
                .should().haveSimpleNameEndingWith("Request")
                .as("Request DTO 클래스명은 'Request'로 끝나야 합니다.");
    }

    /**
     * Response DTO 네이밍 규칙: 클래스명이 Response로 끝나야 합니다.
     */
    public static ArchRule responseDtoNamingRule(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..dto..response..")
                .should().haveSimpleNameEndingWith("Response")
                .as("Response DTO 클래스명은 'Response'로 끝나야 합니다.");
    }

    /**
     * Controller는 @RestController 또는 @Controller 어노테이션이 있어야 합니다.
     */
    public static ArchRule controllerAnnotationRule(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..controller..")
                .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Controller")
                .as("Controller 클래스는 @RestController 또는 @Controller 어노테이션이 필요합니다.");
    }

    /**
     * Service는 @Service 어노테이션이 있어야 합니다.
     */
    public static ArchRule serviceAnnotationRule(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..service..")
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .as("Service 클래스는 @Service 어노테이션이 필요합니다.");
    }

    /**
     * Repository는 @Repository 어노테이션 또는 JpaRepository를 상속해야 합니다.
     */
    public static ArchRule repositoryAnnotationRule(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..repository..")
                .should().beAnnotatedWith("org.springframework.stereotype.Repository")
                .orShould().beAssignableTo("org.springframework.data.jpa.repository.JpaRepository")
                .as("Repository 클래스는 @Repository 또는 JpaRepository를 상속해야 합니다.");
    }

    /**
     * Entity는 @Entity 어노테이션이 있어야 합니다.
     */
    public static ArchRule entityAnnotationRule(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..entity..")
                .and().haveSimpleNameNotContaining("$")
                .should().beAnnotatedWith("jakarta.persistence.Entity")
                .as("Entity 클래스는 @Entity 어노테이션이 필요합니다.");
    }

    /**
     * Entity는 Controller에서 직접 참조하지 못하도록 금지합니다.
     */
    public static ArchRule entityNotUsedInController(String basePackage){
        return noClasses()
                .that().resideInAPackage(basePackage + "..controller..")
                .should().dependOnClassesThat()
                .resideInAPackage(basePackage + "..entity..")
                .as("Controller는 Entity를 직접 참조할 수 없습니다.");
    }

    /**
     * Controller가 다른 서비스의 패키지를 직접 참조하지 못하도록 금지합니다.
     * MSA 경계 보호
     */
    public static ArchRule noDirectServiceCrossImport(
            String sourcePackage, String forbiddenPackage) {
        return noClasses()
                .that().resideInAPackage(sourcePackage + "..")
                .should().dependOnClassesThat()
                .resideInAPackage(forbiddenPackage + "..")
                .as(sourcePackage + "는 " + forbiddenPackage + "를 직접 참조할 수 없습니다.");
    }

    /**
     * DTO 접근 제한 규칙
     * DTO는 Controller와 Service에서만 접근할 수 있습니다.
     */
    public static ArchRule dtoAccessRule(String basePackage) {
        return classes()
                .that().resideInAPackage(basePackage + "..dto..")
                .should().onlyBeAccessed().byClassesThat()
                .resideInAnyPackage(
                        basePackage + "..controller..",
                        basePackage + "..service.."
                )
                .as("DTO는 Controller 또는 Service에서만 접근 가능합니다.");
    }
}