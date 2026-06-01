// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.core

sealed class Result<out T, out E : DomainError> {
    data class Success<T>(
        val value: T,
    ) : Result<T, Nothing>()

    data class Failure<E : DomainError>(
        val error: E,
    ) : Result<Nothing, E>()

    fun <R> map(transform: (T) -> R): Result<R, E> =
        when (this) {
            is Success -> Success(transform(value))
            is Failure -> this
        }

    fun <F : DomainError> mapError(transform: (E) -> F): Result<T, F> =
        when (this) {
            is Success -> this
            is Failure -> Failure(transform(error))
        }

    fun <R> flatMap(transform: (T) -> Result<R, @UnsafeVariance E>): Result<R, E> =
        when (this) {
            is Success -> transform(value)
            is Failure -> this
        }

    fun getOrNull(): T? =
        when (this) {
            is Success -> value
            is Failure -> null
        }

    fun getOrThrow(): T =
        when (this) {
            is Success -> value
            is Failure -> error("Result is Failure: $error")
        }

    fun getOrElse(default: @UnsafeVariance T): T =
        when (this) {
            is Success -> value
            is Failure -> default
        }

    fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (E) -> R,
    ): R =
        when (this) {
            is Success -> onSuccess(value)
            is Failure -> onFailure(error)
        }
}
