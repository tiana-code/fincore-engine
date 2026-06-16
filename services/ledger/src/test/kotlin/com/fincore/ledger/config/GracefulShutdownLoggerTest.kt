// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.support.GenericApplicationContext

class GracefulShutdownLoggerTest {
    @Test
    fun `should emit exactly one info event when the context closes`() {
        val logger = LoggerFactory.getLogger(GracefulShutdownLogger::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        val context = GenericApplicationContext().apply { refresh() }

        try {
            GracefulShutdownLogger().onApplicationEvent(ContextClosedEvent(context))

            appender.list.count { it.level == Level.INFO } shouldBe 1
        } finally {
            logger.detachAppender(appender)
            context.close()
        }
    }
}
