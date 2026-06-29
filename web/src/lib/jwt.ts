// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

export interface JwtClaims {
    username?: string
    tenant?: string
}

// Reads the display claims (user, tenant) from a JWT payload. The signature is NOT
// verified here, that is the API's job; the sandbox only needs the claims to label the
// header. Returns null when no usable token is present.
export function decodeJwtClaims(token: string | undefined): JwtClaims | null {
    if (!token) return null
    const payloadSegment = token.split('.')[1]
    if (!payloadSegment) return null
    try {
        const payload = JSON.parse(decodeBase64Url(payloadSegment)) as Record<string, unknown>
        const claims: JwtClaims = {
            username:
                asNonEmptyString(payload.preferred_username) ?? asNonEmptyString(payload.name),
            tenant: asNonEmptyString(payload.tenant) ?? asNonEmptyString(payload.tenant_id),
        }
        return claims.username || claims.tenant ? claims : null
    } catch {
        return null
    }
}

function decodeBase64Url(value: string): string {
    const base64 = value.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    return atob(padded)
}

function asNonEmptyString(value: unknown): string | undefined {
    return typeof value === 'string' && value.length > 0 ? value : undefined
}
