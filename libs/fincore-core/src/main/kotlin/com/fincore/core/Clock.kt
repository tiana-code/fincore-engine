// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

import java.time.Instant
import java.time.ZoneId

interface ClockProvider {
    fun now(): Instant

    fun zoneId(): ZoneId
}

class SystemClockProvider : ClockProvider {
    private val clock = java.time.Clock.systemUTC()

    override fun now(): Instant = clock.instant()

    override fun zoneId(): ZoneId = clock.zone
}
