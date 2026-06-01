// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    // MapStruct mappers (S4) use kapt, not KSP - MapStruct has no KSP processor.
    // The kapt plugin + kapt(mapstruct-processor) get added when mappers land.
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

    implementation(libs.liquibase.core)
    runtimeOnly(libs.postgres.jdbc)

    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.api)

    testImplementation(project(":libs:fincore-test-support"))
    testImplementation(libs.spring.boot.starter.test)
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

tasks.named("check") {
    dependsOn(integrationTest)
}
