// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

// Group the integer part with thousands separators while preserving the
// fractional digits verbatim. Pure string work - never parse money to a number.
export function formatAmount(amount: string): string {
    const negative = amount.startsWith('-')
    const unsigned = negative ? amount.slice(1) : amount
    const [integer, fraction] = unsigned.split('.')
    const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    return (negative ? '-' : '') + grouped + (fraction ? `.${fraction}` : '')
}

export function formatInstant(iso: string): string {
    return `${iso
        .replace('T', ' ')
        .replace(/\.\d+Z$/, 'Z')
        .replace('Z', ' UTC')}`
}
