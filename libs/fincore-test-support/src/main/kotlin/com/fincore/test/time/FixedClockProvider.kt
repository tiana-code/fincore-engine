// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.test.time

import com.fincore.core.ClockProvider
import java.time.Instant
import java.time.ZoneId

class FixedClockProvider(
    private val fixed: Instant,
    private val zone: ZoneId = ZoneId.of("UTC"),
) : ClockProvider {
    override fun now(): Instant = fixed

    override fun zoneId(): ZoneId = zone

    companion object {
        fun at(iso: String): FixedClockProvider = FixedClockProvider(Instant.parse(iso))

        fun atEpoch(): FixedClockProvider = FixedClockProvider(Instant.EPOCH)
    }
}
