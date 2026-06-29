// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { LedgerActivityEvent, LedgerActivityType } from '@/api/types'
import type { IconName } from '@/components/Icon'
import { formatMoney } from '@/lib/format'
import { formatRelative } from '@/lib/relativeTime'
import type { Tone } from '@/lib/tone'
import type { ActivityEvent } from '@/mock/types'

const VIEW: Record<LedgerActivityType, { icon: IconName; tone: Tone }> = {
    'transaction.posted': { icon: 'arrows', tone: 'neutral' },
    'transaction.reversed': { icon: 'arrows', tone: 'amber' },
    'account.created': { icon: 'wallet', tone: 'credit' },
}

// Maps a ledger event to the display model: id, optional money, and label, with a
// relative timestamp from the given clock (defaults to the wall clock).
export function toActivityEvent(event: LedgerActivityEvent, now?: number): ActivityEvent {
    const view = VIEW[event.type]
    const parts = [event.resourceId]
    if (event.amount && event.currency) parts.push(formatMoney(event.amount, event.currency))
    parts.push(event.label)
    return {
        type: event.type,
        detail: parts.join(' · '),
        ts: formatRelative(event.occurredAt, now),
        icon: view.icon,
        tone: view.tone,
    }
}
