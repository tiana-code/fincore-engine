// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { ReactNode } from 'react'
import { Icon, type IconName } from './Icon'

interface StateMessageProps {
    icon: IconName
    text: string
    children?: ReactNode
}

export function StateMessage({ icon, text, children }: StateMessageProps) {
    return (
        <div
            style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 10,
                padding: '64px 24px',
                color: 'var(--text-3)',
                fontSize: 13,
            }}
        >
            <Icon name={icon} size={20} />
            <span>{text}</span>
            {children}
        </div>
    )
}
