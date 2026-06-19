// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiPost } from '@/api/client'
import type { PaymentResponse } from '@/api/types'

export interface NewPayment {
    amount: number
    currency: string
    reference: string
}

export function useInitiatePayment() {
    const queryClient = useQueryClient()
    return useMutation({
        // The key is supplied by the caller (one per user intent) so a retry of the same submit reuses it and the
        // backend dedupes it, rather than minting a fresh key and creating a duplicate payment.
        mutationFn: ({
            payment,
            idempotencyKey,
        }: {
            payment: NewPayment
            idempotencyKey: string
        }) =>
            apiPost<PaymentResponse>('/v1/payments', {
                body: payment,
                headers: { 'Idempotency-Key': idempotencyKey },
            }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['payments'] }),
    })
}
