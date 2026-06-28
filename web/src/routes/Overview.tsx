// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { Kpi } from '@/components/Kpi'
import { Shell } from '@/components/Shell'
import { ActivityFeed } from '@/features/overview/ActivityFeed'
import { OverviewTopBar } from '@/features/overview/OverviewTopBar'
import { SystemLinks } from '@/features/overview/SystemLinks'
import { TryTheApi } from '@/features/overview/TryTheApi'
import { getOverview } from '@/mock/overview'

export function Overview() {
    const data = getOverview()
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
                        border: '1px solid var(--amber)',
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
                            background: 'var(--amber)',
                            flexShrink: 0,
                        }}
                    />
                    <span>
                        <strong style={{ color: 'var(--text)' }}>Demo overview.</strong> FinCore is
                        fully functional and serves a live API; the other screens read real data.
                        The headline metrics and activity feed on this landing page are sample
                        values for demonstration, not a live feed.
                    </span>
                </div>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
                    {data.kpis.map((kpi) => (
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
