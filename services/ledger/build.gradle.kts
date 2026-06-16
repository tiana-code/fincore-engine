// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.kapt)
}

description = "FinCore Ledger service: double-entry accounts, transactions, balances"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":libs:fincore-core"))
    implementation(project(":libs:fincore-events"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi.starter)

    implementation(libs.mapstruct.core)
    kapt(libs.mapstruct.processor)

    implementation(libs.liquibase.core)
    runtimeOnly(libs.postgres.jdbc)

    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.tracing.bridge.otel)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.exporter.otlp)

    testImplementation(project(":libs:fincore-test-support"))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.testcontainers.junit5)
}

tasks.test {
    useJUnitPlatform()
}

// Integration tests (Spring Boot + Testcontainers) live in src/integrationTest.
// Kept in a separate source set and task so they run apart from fast unit tests.
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

configurations["integrationTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val integrationTest =
    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests (Spring Boot + Testcontainers)."
        group = "verification"
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        useJUnitPlatform()
        shouldRunAfter(tasks.named("test"))
    }

// Domain coverage gate (#53): enforce a minimum on com.fincore.ledger.domain only, from the
// fast unit `test` exec data (no Docker/integrationTest). The rule is aggregate over the whole
// domain bundle, so a single Kotlin synthetic branch cannot swing it; both counters sit at 1.000
// today with margin. See docs/plans/domain-coverage-gate/plan.md (DR-7).
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(
            classDirectories.files.map { dir ->
                fileTree(dir) {
                    include("**/com/fincore/ledger/domain/**")
                    exclude("**/*MapperImpl*", "**/config/**")
                }
            },
        ),
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(integrationTest)
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
