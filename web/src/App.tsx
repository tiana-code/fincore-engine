// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { ThemeProvider } from '@/components/ThemeProvider'
import { AccountDetail } from '@/routes/AccountDetail'
import { Accounts } from '@/routes/Accounts'
import { Cases } from '@/routes/Cases'
import { Overview } from '@/routes/Overview'
import { TransactionDetail } from '@/routes/TransactionDetail'
import { Transactions } from '@/routes/Transactions'

const queryClient = new QueryClient()

export function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <ThemeProvider>
                <BrowserRouter>
                    <Routes>
                        <Route path="/" element={<Overview />} />
                        <Route path="/accounts" element={<Accounts />} />
                        <Route path="/accounts/:id" element={<AccountDetail />} />
                        <Route path="/transactions" element={<Transactions />} />
                        <Route path="/transactions/:id" element={<TransactionDetail />} />
                        <Route path="/compliance/cases" element={<Cases />} />
                    </Routes>
                </BrowserRouter>
            </ThemeProvider>
        </QueryClientProvider>
    )
}
