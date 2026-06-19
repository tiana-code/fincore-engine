// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useState } from 'react'
import type { NewPayment } from './useInitiatePayment'

const CURRENCIES = ['USD', 'EUR', 'GBP']
const MAX_REFERENCE = 140

interface PaymentFormProps {
    pending: boolean
    onSubmit: (payment: NewPayment) => void
    onCancel: () => void
}

export function PaymentForm({ pending, onSubmit, onCancel }: PaymentFormProps) {
    const [amount, setAmount] = useState('')
    const [currency, setCurrency] = useState(CURRENCIES[0])
    const [reference, setReference] = useState('')

    const amountValue = Number(amount)
    const valid =
        amount.trim() !== '' &&
        Number.isFinite(amountValue) &&
        amountValue > 0 &&
        reference.trim().length > 0

    const submit = (event: React.FormEvent) => {
        event.preventDefault()
        if (!valid) return
        onSubmit({ amount: amountValue, currency, reference: reference.trim() })
    }

    return (
        <form onSubmit={submit} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <input
                className="input"
                aria-label="Amount"
                inputMode="decimal"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                placeholder="Amount"
            />
            <select
                className="input"
                aria-label="Currency"
                value={currency}
                onChange={(event) => setCurrency(event.target.value)}
            >
                {CURRENCIES.map((code) => (
                    <option key={code} value={code}>
                        {code}
                    </option>
                ))}
            </select>
            <input
                className="input mono"
                aria-label="Reference"
                maxLength={MAX_REFERENCE}
                value={reference}
                onChange={(event) => setReference(event.target.value)}
                placeholder="Reference"
                style={{ flex: 1 }}
            />
            <button type="submit" className="btn btn-primary" disabled={!valid || pending}>
                Create
            </button>
            <button type="button" className="btn" onClick={onCancel} disabled={pending}>
                Cancel
            </button>
        </form>
    )
}
