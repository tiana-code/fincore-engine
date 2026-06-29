// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { IconName } from '@/components/Icon'
import type { Tone } from '@/lib/tone'

export interface KpiSegment {
    text: string
    tone: Tone
}

export interface KpiDatum {
    label: string
    value: string
    sub: KpiSegment[]
    // Live KPIs derived from the API omit the sparkline: a single count has no
    // time series, so a trend line would be fabricated. Sample KPIs keep it.
    spark?: number[]
    sparkColor?: string
}

export interface ActivityEvent {
    type: string
    detail: string
    ts: string
    icon: IconName
    tone: Tone
}

export interface ApiExample {
    title: string
    endpoint: string
    icon: IconName
    curl: string
}

export interface SystemLink {
    title: string
    sub: string
    url: string
    icon: IconName
}
