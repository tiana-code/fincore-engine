// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

// Deterministic noisy series (Math.sin based, no Math.random) so renders and tests are stable.
export function genSeries(n: number, base: number, vol = 0.08, trend = 0.002): number[] {
  let v = base
  const out: number[] = []
  for (let i = 0; i < n; i++) {
    v = v * (1 + (Math.sin(i * 0.7 + base) * 0.5 + ((i % 7) - 3) / 30) * vol + trend)
    out.push(v)
  }
  return out
}
