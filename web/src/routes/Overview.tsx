// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { ReactNode } from 'react'
import { Kpi } from '@/components/Kpi'
import { Shell } from '@/components/Shell'
import { toActivityEvent } from '@/features/overview/activityView'
import { ActivityFeed, type ActivityState } from '@/features/overview/ActivityFeed'
import { liveKpis, placeholderKpis } from '@/features/overview/liveKpis'
import { OverviewTopBar, type OverviewMode } from '@/features/overview/OverviewTopBar'
import { API_EXAMPLES, SYSTEM_LINKS } from '@/features/overview/overviewConfig'
import { SystemLinks } from '@/features/overview/SystemLinks'
import { TryTheApi } from '@/features/overview/TryTheApi'
import { useLedgerOverview } from '@/features/overview/useLedgerOverview'
import { useOverviewMetrics } from '@/features/overview/useOverviewMetrics'

const NOTICE: Record<OverviewMode, { color: string; body: ReactNode }> = {
    live: {
        color: 'var(--accent)',
        body: (
            <>
                <strong style={{ color: 'var(--text)' }}>Live data.</strong> The headline counts and
                the recent-activity feed are read from the sandbox ledger API.
            </>
        ),
    },
    loading: {
        color: 'var(--text-3)',
        body: <>Connecting to the sandbox API...</>,
    },
    offline: {
        color: 'var(--amber)',
        body: (
            <>
                <strong style={{ color: 'var(--text)' }}>Sandbox API not reachable.</strong> Start
                the local stack to see live data.
            </>
        ),
    },
}

export function Overview() {
    const { isError: metricsError, metrics } = useOverviewMetrics()
    const overview = useLedgerOverview()

    const mode: OverviewMode = metrics ? 'live' : metricsError ? 'offline' : 'loading'
    const kpis = metrics
        ? liveKpis(metrics, overview.data?.transactionsLast24h)
        : placeholderKpis(mode === 'offline' ? 'unavailable' : 'loading...')
    const notice = NOTICE[mode]

    const activityState: ActivityState = overview.data
        ? 'ready'
        : overview.isError
          ? 'offline'
          : 'loading'
    const activity = overview.data?.activity.map((event) => toActivityEvent(event)) ?? []

    return (
        <Shell activeNav="Overview" topBar={<OverviewTopBar mode={mode} />}>
            <div className="flex flex-col gap-[18px] px-6 pb-8 pt-6">
                <div
                    role="note"
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 10,
                        padding: '10px 14px',
                        borderRadius: 10,
                        border: `1px solid ${notice.color}`,
                        background: 'var(--surface)',
                        fontSize: 12.5,
                        color: 'var(--text-2)',
                    }}
                >
                    <span
                        style={{
                            width: 7,
                            height: 7,
                            borderRadius: '50%',
                            background: notice.color,
                            flexShrink: 0,
                        }}
                    />
                    <span>{notice.body}</span>
                </div>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
                    {kpis.map((kpi) => (
                        <Kpi key={kpi.label} datum={kpi} />
                    ))}
                </div>
                <div className="grid grid-cols-1 gap-[18px] lg:grid-cols-2">
                    <ActivityFeed activity={activity} state={activityState} />
                    <TryTheApi examples={API_EXAMPLES} />
                </div>
                <SystemLinks links={SYSTEM_LINKS} />
            </div>
        </Shell>
    )
}
