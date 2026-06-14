// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { AccountStatus, AccountType } from '@/api/types'

const STATUS_CLASS: Record<AccountStatus, string> = {
    ACTIVE: 'pill-teal',
    FROZEN: 'pill-amber',
    CLOSED: 'pill-grey',
}

export function StatusPill({ status }: { status: AccountStatus }) {
    return (
        <span className={`pill ${STATUS_CLASS[status] ?? 'pill-grey'}`}>
            <span className="dot" />
            {status}
        </span>
    )
}

export function TypePill({ type }: { type: AccountType }) {
    return (
        <span className="pill pill-grey mono" style={{ fontSize: 10 }}>
            {type}
        </span>
    )
}
