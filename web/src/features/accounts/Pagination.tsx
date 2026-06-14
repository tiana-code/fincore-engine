// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { Icon } from '@/components/Icon'

interface PaginationProps {
    page: number
    totalPages: number
    totalElements: number
    onPrev: () => void
    onNext: () => void
    noun?: string
}

export function Pagination({
    page,
    totalPages,
    totalElements,
    onPrev,
    onNext,
    noun = 'accounts',
}: PaginationProps) {
    const prevDisabled = page <= 0
    const nextDisabled = totalPages === 0 || page >= totalPages - 1
    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '10px 16px',
                borderTop: '1px solid var(--border)',
                fontSize: 11.5,
                color: 'var(--text-2)',
            }}
        >
            <span>
                <span className="mono" style={{ color: 'var(--text)' }}>
                    {totalElements}
                </span>{' '}
                {noun}
            </span>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <button
                    type="button"
                    className="btn btn-sm"
                    onClick={onPrev}
                    disabled={prevDisabled}
                    aria-label="Previous page"
                >
                    <Icon name="chevLeft" size={11} />
                </button>
                <span className="mono">
                    page {page + 1} of {Math.max(totalPages, 1)}
                </span>
                <button
                    type="button"
                    className="btn btn-sm"
                    onClick={onNext}
                    disabled={nextDisabled}
                    aria-label="Next page"
                >
                    <Icon name="chevRight" size={11} />
                </button>
            </div>
        </div>
    )
}
