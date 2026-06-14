// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import {
    Activity,
    AlertTriangle,
    ArrowDownUp,
    BookOpen,
    Check,
    ChevronDown,
    ChevronLeft,
    ChevronRight,
    Code,
    Copy,
    Home,
    Inbox,
    Key,
    Play,
    Scale,
    Search,
    Send,
    Shield,
    Wallet,
    type LucideIcon,
} from 'lucide-react'

const ICONS = {
    home: Home,
    wallet: Wallet,
    arrows: ArrowDownUp,
    send: Send,
    scale: Scale,
    shield: Shield,
    book: BookOpen,
    search: Search,
    chevDown: ChevronDown,
    chevLeft: ChevronLeft,
    chevRight: ChevronRight,
    check: Check,
    key: Key,
    code: Code,
    activity: Activity,
    copy: Copy,
    play: Play,
    alert: AlertTriangle,
    inbox: Inbox,
} satisfies Record<string, LucideIcon>

export type IconName = keyof typeof ICONS

interface IconProps {
    name: IconName
    size?: number
    className?: string
    style?: React.CSSProperties
}

export function Icon({ name, size = 14, className, style }: IconProps) {
    const Glyph = ICONS[name]
    return <Glyph size={size} strokeWidth={1.6} className={className} style={style} aria-hidden />
}
