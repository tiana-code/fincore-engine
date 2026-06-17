// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

description = "FinCore Decision Engine core: typed JSON predicate DSL, parser, deterministic evaluator"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.jackson.databind)

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "com.fincore"
            artifactId = "decision-engine"
            version = rootProject.version.toString()
            pom {
                name.set("FinCore Decision Engine")
                description.set(
                    "Typed JSON predicate DSL, fail-closed parser and deterministic evaluator for " +
                        "FinCore Engine; embeddable without the decision service.",
                )
                url.set("https://github.com/tiana-code/fincore-engine")
                licenses {
                    license {
                        name.set("Business Source License 1.1")
                        url.set("https://github.com/tiana-code/fincore-engine/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("tiana-code")
                        name.set("Tiana")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/tiana-code/fincore-engine.git")
                    developerConnection.set("scm:git:ssh://git@github.com/tiana-code/fincore-engine.git")
                    url.set("https://github.com/tiana-code/fincore-engine")
                }
            }
        }
    }
}
