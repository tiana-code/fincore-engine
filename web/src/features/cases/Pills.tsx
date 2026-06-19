// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { CaseStatus } from '@/api/types'

const STATUS_CLASS: Record<CaseStatus, string> = {
    OPEN: 'pill-teal',
    CLAIMED: 'pill-violet',
    ESCALATED: 'pill-amber',
    RESOLVED: 'pill-grey',
}

export function CaseStatusPill({ status }: { status: CaseStatus }) {
    return (
        <span className={`pill ${STATUS_CLASS[status] ?? 'pill-grey'}`}>
            <span className="dot" />
            {status}
        </span>
    )
}
