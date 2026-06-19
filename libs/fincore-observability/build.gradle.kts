// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

description = "FinCore observability: structured-log PII masking shared across services"

kotlin {
    jvmToolchain(21)
}

// Boot BOM via Gradle's native platform() so version-less Spring deps resolve, scoped to
// the dependency configurations only (does not apply the BOM to detekt's configuration).
val springBootBom = "org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"

dependencies {
    implementation(platform(springBootBom))
    implementation(libs.kotlin.stdlib)

    // spring-boot-starter brings spring-boot core (StructuredLoggingJsonMembersCustomizer / JsonWriter)
    // and logback-classic (ILoggingEvent) transitively.
    implementation(libs.spring.boot.starter)

    testImplementation(platform(springBootBom))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.jackson.module.kotlin)
}

tasks.test {
    useJUnitPlatform()
}
