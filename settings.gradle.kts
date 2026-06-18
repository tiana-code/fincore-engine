// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
    // The `libs` version catalog is auto-created by Gradle from
    // gradle/libs.versions.toml (conventional path). Do NOT declare it manually
    // here - that triggers "you can only call the 'from' method a single time".
}

rootProject.name = "fincore-engine"

include(
    ":libs:fincore-core",
    ":libs:fincore-events",
    ":libs:fincore-eventbus",
    ":libs:fincore-test-support",
    ":libs:decision-engine",
    ":services:ledger",
    ":services:decision",
    ":services:payments",
)
