// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { ReactNode } from 'react'
import { Sidebar } from './Sidebar'

interface ShellProps {
  activeNav?: string
  topBar?: ReactNode
  children: ReactNode
}

export function Shell({ activeNav = 'Overview', topBar, children }: ShellProps) {
  return (
    <div style={{ display: 'flex', height: '100vh', width: '100vw', overflow: 'hidden' }}>
      <Sidebar active={activeNav} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        {topBar}
        <div className="fc-scroll" style={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
          {children}
        </div>
      </div>
    </div>
  )
}
