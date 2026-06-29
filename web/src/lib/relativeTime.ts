// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

// Compact "time ago" label. The reference clock is a parameter so renders and tests
// are deterministic; it defaults to the wall clock.
export function formatRelative(iso: string, now: number = Date.now()): string {
    const seconds = Math.max(0, Math.round((now - new Date(iso).getTime()) / 1000))
    if (seconds < 60) return `${seconds}s ago`
    const minutes = Math.floor(seconds / 60)
    if (minutes < 60) return `${minutes}m ago`
    const hours = Math.floor(minutes / 60)
    if (hours < 24) return `${hours}h ago`
    return `${Math.floor(hours / 24)}d ago`
}
