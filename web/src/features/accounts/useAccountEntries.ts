// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useInfiniteQuery } from '@tanstack/react-query'
import { apiFetch } from '@/api/client'
import type { EntryPageResponse } from '@/api/types'

export function useAccountEntries(id: string) {
    return useInfiniteQuery({
        queryKey: ['account', id, 'entries'],
        queryFn: ({ pageParam }) =>
            apiFetch<EntryPageResponse>(
                `/v1/accounts/${id}/entries${pageParam ? `?cursor=${pageParam}` : ''}`,
            ),
        initialPageParam: '',
        getNextPageParam: (last) => last.nextCursor ?? undefined,
    })
}
