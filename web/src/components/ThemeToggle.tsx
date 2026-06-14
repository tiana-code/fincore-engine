// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useTheme } from './ThemeProvider'

export function ThemeToggle() {
  const { theme, toggle } = useTheme()
  return (
    <button className="btn btn-sm" onClick={toggle} aria-label="Toggle color theme">
      {theme === 'dark' ? 'Light' : 'Dark'}
    </button>
  )
}
