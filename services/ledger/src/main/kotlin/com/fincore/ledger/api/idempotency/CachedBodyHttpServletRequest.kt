// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.idempotency

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

class CachedBodyHttpServletRequest(
    request: HttpServletRequest,
) : HttpServletRequestWrapper(request) {
    private val cachedBody: ByteArray = request.inputStream.readBytes()

    fun body(): ByteArray = cachedBody

    override fun getInputStream(): ServletInputStream = CachedBodyServletInputStream(cachedBody)

    override fun getReader(): BufferedReader =
        BufferedReader(InputStreamReader(ByteArrayInputStream(cachedBody), characterEncoding ?: Charsets.UTF_8.name()))
}

private class CachedBodyServletInputStream(
    body: ByteArray,
) : ServletInputStream() {
    private val buffer = ByteArrayInputStream(body)

    override fun read(): Int = buffer.read()

    override fun isFinished(): Boolean = buffer.available() == 0

    override fun isReady(): Boolean = true

    override fun setReadListener(listener: ReadListener?): Unit = throw UnsupportedOperationException("async read unsupported")
}
