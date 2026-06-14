// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

export interface SparklinePaths {
  line: string
  fill: string
}

export function buildSparkline(data: number[], w: number, h: number): SparklinePaths {
  const min = Math.min(...data)
  const max = Math.max(...data)
  const span = max - min || 1
  const pts = data.map(
    (v, i) => [(i / (data.length - 1)) * w, h - 2 - ((v - min) / span) * (h - 4)] as const,
  )
  const line = pts.map(([x, y], i) => `${i ? 'L' : 'M'}${x.toFixed(1)} ${y.toFixed(1)}`).join(' ')
  return { line, fill: `${line} L ${w} ${h} L 0 ${h} Z` }
}

interface SparklineProps {
  data: number[]
  w?: number
  h?: number
  color?: string
  fill?: boolean
}

export function Sparkline({ data, w = 80, h = 28, color = 'var(--accent)', fill = true }: SparklineProps) {
  const { line, fill: fillD } = buildSparkline(data, w, h)
  return (
    <svg width={w} height={h} className="spark" aria-hidden>
      {fill && <path d={fillD} fill={color} opacity={0.12} />}
      <path d={line} fill="none" stroke={color} strokeWidth={1.3} strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  )
}
