// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { IconName } from '@/components/Icon'
import type { Tone } from '@/lib/tone'

export interface ServiceHealth {
  name: string
  version: string
  ok: boolean
}

export interface KpiSegment {
  text: string
  tone: Tone
}

export interface KpiDatum {
  label: string
  value: string
  sub: KpiSegment[]
  spark: number[]
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

export interface SandboxStatus {
  title: string
  version: string
  build: string
  uptime: string
  user: string
  tenant: string
}

export interface OverviewData {
  status: SandboxStatus
  services: ServiceHealth[]
  kpis: KpiDatum[]
  activity: ActivityEvent[]
  apiExamples: ApiExample[]
  systemLinks: SystemLink[]
}
