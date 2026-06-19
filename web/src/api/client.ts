// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
const DEV_BEARER = import.meta.env.VITE_DEV_BEARER_TOKEN

export class ApiError extends Error {
    constructor(readonly status: number) {
        super(`request failed with status ${status}`)
        this.name = 'ApiError'
    }
}

export async function apiFetch<T>(path: string): Promise<T> {
    const headers: Record<string, string> = { Accept: 'application/json' }
    if (DEV_BEARER) headers.Authorization = `Bearer ${DEV_BEARER}`
    const response = await fetch(`${BASE_URL}${path}`, { headers })
    if (!response.ok) throw new ApiError(response.status)
    return (await response.json()) as T
}

export async function apiPost<T>(
    path: string,
    options: { body?: unknown; headers?: Record<string, string> } = {},
): Promise<T> {
    const headers: Record<string, string> = { Accept: 'application/json', ...options.headers }
    if (DEV_BEARER) headers.Authorization = `Bearer ${DEV_BEARER}`
    const init: RequestInit = { method: 'POST', headers }
    if (options.body !== undefined) {
        headers['Content-Type'] = 'application/json'
        init.body = JSON.stringify(options.body)
    }
    const response = await fetch(`${BASE_URL}${path}`, init)
    if (!response.ok) throw new ApiError(response.status)
    return (await response.json()) as T
}
