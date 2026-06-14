// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { TransactionStatus } from '@/api/types'

const STATUS_CLASS: Record<TransactionStatus, string> = {
    POSTED: 'pill-teal',
    REVERSED: 'pill-grey',
}

export function TxStatusPill({ status }: { status: TransactionStatus }) {
    return (
        <span className={`pill ${STATUS_CLASS[status] ?? 'pill-grey'}`}>
            <span className="dot" />
            {status}
        </span>
    )
}
