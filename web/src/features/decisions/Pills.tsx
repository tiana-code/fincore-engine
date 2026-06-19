// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

export function MatchedPill({ matched }: { matched: boolean }) {
    return (
        <span className={`pill ${matched ? 'pill-teal' : 'pill-grey'}`}>
            <span className="dot" />
            {matched ? 'MATCHED' : 'NO MATCH'}
        </span>
    )
}
