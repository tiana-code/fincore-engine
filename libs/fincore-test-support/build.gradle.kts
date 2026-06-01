// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

plugins {
    alias(libs.plugins.kotlin.jvm)
}

description = "FinCore test support: data builders, Testcontainers extensions, fixed clocks"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":libs:fincore-core"))
    implementation(project(":libs:fincore-events"))

    // Test infra is API of this module — exposed to consumers
    api(libs.testcontainers.core)
    api(libs.testcontainers.junit5)
    api(libs.testcontainers.postgres)
    api(libs.testcontainers.redpanda)
    api(libs.testcontainers.keycloak)
    api(libs.junit.jupiter.api)
    api(libs.kotest.assertions.core)
    api(libs.kotest.property)
    api(libs.mockk)

    testImplementation(libs.kotest.runner.junit5)
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}
