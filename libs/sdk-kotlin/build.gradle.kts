// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

description = "FinCore Kotlin SDK: a typed HTTP client over the FinCore Engine REST API"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.databind)

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.runner.junit5)
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
            artifactId = "sdk-kotlin"
            version = rootProject.version.toString()
            pom {
                name.set("FinCore Kotlin SDK")
                description.set(
                    "Typed JVM client over the FinCore Engine REST API, depending only on the JDK " +
                        "HTTP client and Jackson.",
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
