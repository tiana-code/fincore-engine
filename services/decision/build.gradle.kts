// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

description = "FinCore Decision service: rule and immutable rule-version storage"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.liquibase.core)
    runtimeOnly(libs.postgres.jdbc)

    testImplementation(project(":libs:fincore-test-support"))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.testcontainers.junit5)
}

tasks.test {
    useJUnitPlatform()
}

// Integration tests (Spring Boot + Testcontainers) live in src/integrationTest,
// kept in a separate source set and task so they run apart from fast unit tests.
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

configurations["integrationTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

dependencies {
    "integrationTestImplementation"(libs.postgres.jdbc)
}

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
