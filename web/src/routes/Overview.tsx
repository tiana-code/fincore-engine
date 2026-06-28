// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { Kpi } from '@/components/Kpi'
import { Shell } from '@/components/Shell'
import { ActivityFeed } from '@/features/overview/ActivityFeed'
import { liveKpis } from '@/features/overview/liveKpis'
import { OverviewTopBar } from '@/features/overview/OverviewTopBar'
import { SystemLinks } from '@/features/overview/SystemLinks'
import { TryTheApi } from '@/features/overview/TryTheApi'
import { useOverviewMetrics } from '@/features/overview/useOverviewMetrics'
import { getOverview } from '@/mock/overview'

const NOTICE = {
    live: {
        color: 'var(--accent)',
        body: (
            <>
                <strong style={{ color: 'var(--text)' }}>Live metrics.</strong> The headline counts
                are read from the sandbox API. The recent-activity feed below is an illustrative
                sample, not a live event stream.
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
                <strong style={{ color: 'var(--text)' }}>Sandbox API not reachable.</strong> The
                headline counts below are sample values; start the local stack to see live data. The
                activity feed is always an illustrative sample.
            </>
        ),
    },
} as const

export function Overview() {
    const { isError, metrics } = useOverviewMetrics()
    const data = getOverview()

    const mode = metrics ? 'live' : isError ? 'offline' : 'loading'
    const kpis = metrics ? liveKpis(metrics) : data.kpis
    const notice = NOTICE[mode]

    return (
        <Shell
            activeNav="Overview"
            topBar={<OverviewTopBar status={data.status} services={data.services} />}
        >
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
                    <ActivityFeed activity={data.activity} />
                    <TryTheApi examples={data.apiExamples} />
                </div>
                <SystemLinks links={data.systemLinks} />
            </div>
        </Shell>
    )
}
