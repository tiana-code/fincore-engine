// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.eval

/**
 * Raised when a regex match is aborted because the evaluating thread was interrupted. Lets a caller bound
 * the time spent matching an author-supplied pattern against input (catastrophic backtracking) by scheduling
 * an interrupt; the match unwinds at the next character read instead of running to completion.
 */
class RegexMatchInterruptedException : RuntimeException("regex match interrupted")

/**
 * A transparent view over a [CharSequence] that checks the thread's interrupt flag on every character read.
 * The regex engine re-reads characters while backtracking, so wrapping the match input makes an otherwise
 * uninterruptible [java.util.regex] match abortable. Behaviour is identical to the wrapped sequence whenever
 * the thread is not interrupted, so evaluation stays deterministic for non-pathological inputs.
 */
internal class InterruptibleCharSequence(
    private val inner: CharSequence,
) : CharSequence {
    override val length: Int get() = inner.length

    override fun get(index: Int): Char {
        if (Thread.currentThread().isInterrupted) throw RegexMatchInterruptedException()
        return inner[index]
    }

    override fun subSequence(
        startIndex: Int,
        endIndex: Int,
    ): CharSequence = InterruptibleCharSequence(inner.subSequence(startIndex, endIndex))

    override fun toString(): String = inner.toString()
}
