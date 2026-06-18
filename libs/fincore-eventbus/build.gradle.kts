// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

description = "FinCore event bus: Kafka/Redpanda producer and admin auto-configuration"

kotlin {
    jvmToolchain(21)
}

// Import the Spring Boot BOM via Gradle's native platform() so version-less Spring
// dependencies (spring-kafka) resolve. Scoped to the dependency configurations only -
// unlike the io.spring.dependency-management plugin, this does NOT apply the BOM to the
// detekt configuration (which would downgrade detekt's bundled Kotlin compiler).
val springBootBom = "org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"

dependencies {
    implementation(platform(springBootBom))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.kafka)

    testImplementation(platform(springBootBom))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
}

tasks.test {
    useJUnitPlatform()
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

configurations["integrationTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

dependencies {
    "integrationTestImplementation"(platform(springBootBom))
    "integrationTestImplementation"(libs.testcontainers.core)
    "integrationTestImplementation"(libs.testcontainers.redpanda)
    "integrationTestImplementation"(libs.testcontainers.junit5)
}

val integrationTest =
    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests (Spring config + Testcontainers Redpanda)."
        group = "verification"
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        useJUnitPlatform()
        shouldRunAfter(tasks.named("test"))
    }

tasks.named("check") {
    dependsOn(integrationTest)
}

java {
    withSourcesJar()
    withJavadocJar()
}
