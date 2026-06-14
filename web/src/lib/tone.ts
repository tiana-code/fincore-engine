// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

export type Tone = 'accent' | 'credit' | 'debit' | 'amber' | 'violet' | 'neutral'

export function toneColor(tone: Tone): string {
  switch (tone) {
    case 'accent':
      return 'var(--accent)'
    case 'credit':
      return 'var(--credit)'
    case 'debit':
      return 'var(--text-err)'
    case 'amber':
      return 'var(--amber)'
    case 'violet':
      return 'var(--violet)'
    default:
      return 'var(--text-2)'
  }
}
