// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.consumer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Testcontainers
class JdbcProcessedEventStoreIT {
    private val jdbcTemplate = JdbcTemplate(dataSource)
    private val transactionTemplate = TransactionTemplate(DataSourceTransactionManager(dataSource))
    private val store = JdbcProcessedEventStore(jdbcTemplate)

    @BeforeEach
    fun resetTable() {
        val ddl = ClassPathResource("db/processed-events.sql").inputStream.bufferedReader().use { it.readText() }
        jdbcTemplate.execute(ddl)
        jdbcTemplate.execute("TRUNCATE processed_events")
    }

    @Test
    fun `should return first-seen then duplicate and persist a single row`() {
        val id = UUID.randomUUID()

        store.markIfFirstSeen(id, "group") shouldBe true
        store.markIfFirstSeen(id, "group") shouldBe false
        rowCount(id) shouldBe 1
    }

    @Test
    fun `should undo the claim when the surrounding transaction rolls back`() {
        val id = UUID.randomUUID()

        transactionTemplate.executeWithoutResult { status ->
            store.markIfFirstSeen(id, "group")
            status.setRollbackOnly()
        }

        rowCount(id) shouldBe 0
        store.markIfFirstSeen(id, "group") shouldBe true
    }

    @Test
    fun `should grant first-seen to exactly one of two concurrent transactions`() {
        val id = UUID.randomUUID()
        val results = CopyOnWriteArrayList<Boolean>()
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)
        val pool = Executors.newFixedThreadPool(2)
        repeat(2) {
            pool.submit {
                start.await()
                transactionTemplate.executeWithoutResult { results.add(store.markIfFirstSeen(id, "group")) }
                done.countDown()
            }
        }

        start.countDown()
        done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        pool.shutdown()

        results.size shouldBe 2
        results.count { it } shouldBe 1
        rowCount(id) shouldBe 1
    }

    private fun rowCount(id: UUID): Int =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM processed_events WHERE envelope_id = ?",
            Int::class.java,
            id,
        ) ?: 0

    companion object {
        private const val TIMEOUT_SECONDS = 10L

        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:17-alpine")

        private val dataSource: PGSimpleDataSource by lazy {
            PGSimpleDataSource().apply {
                setUrl(postgres.jdbcUrl)
                user = postgres.username
                password = postgres.password
            }
        }
    }
}
