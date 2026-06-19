// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiPost } from '@/api/client'
import type { CaseResponse, CaseStatus } from '@/api/types'

export type CaseAction = 'claim' | 'resolve' | 'escalate'

export function actionsFor(status: CaseStatus): CaseAction[] {
    switch (status) {
        case 'OPEN':
            return ['claim']
        case 'CLAIMED':
            return ['resolve', 'escalate']
        case 'ESCALATED':
            return ['resolve']
        case 'RESOLVED':
            return []
    }
}

export function useCaseAction() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ id, action }: { id: string; action: CaseAction }) =>
            apiPost<CaseResponse>(`/v1/compliance/cases/${id}/${action}`),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cases'] }),
    })
}
