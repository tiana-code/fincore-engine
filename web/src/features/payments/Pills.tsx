// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { PaymentStatus } from '@/api/types'

const STATUS_CLASS: Record<PaymentStatus, string> = {
    INITIATED: 'pill-grey',
    SCREENING: 'pill-amber',
    SUBMITTED: 'pill-violet',
    SETTLED: 'pill-teal',
    FAILED: 'pill-amber',
    CANCELLED: 'pill-grey',
}

export function PaymentStatusPill({ status }: { status: PaymentStatus }) {
    return (
        <span className={`pill ${STATUS_CLASS[status] ?? 'pill-grey'}`}>
            <span className="dot" />
            {status}
        </span>
    )
}
