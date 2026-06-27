import type { OnboardingSessionResponse, SubmitDetailsRequest } from '../types/onboarding'

const BASE = '/api/onboarding/sessions'

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    const body = await res.json().catch(() => null)
    const err: { code: string; message: string } =
      body && body.code
        ? body
        : { code: 'UNKNOWN', message: 'An unexpected error occurred.' }
    throw err
  }
  return res.json()
}

export async function createSession(): Promise<OnboardingSessionResponse> {
  return request(BASE, { method: 'POST' })
}

export async function getSession(sessionId: string): Promise<OnboardingSessionResponse> {
  return request(`${BASE}/${sessionId}`)
}

export async function submitDetails(
  sessionId: string,
  details: SubmitDetailsRequest,
): Promise<OnboardingSessionResponse> {
  return request(`${BASE}/${sessionId}/details`, {
    method: 'PUT',
    body: JSON.stringify(details),
  })
}

export async function triggerValidation(
  sessionId: string,
  apiKey: string,
): Promise<OnboardingSessionResponse> {
  return request(`${BASE}/${sessionId}/validation`, {
    method: 'POST',
    body: JSON.stringify({ apiKey }),
  })
}

export async function goLive(sessionId: string): Promise<OnboardingSessionResponse> {
  return request(`${BASE}/${sessionId}/go-live`, { method: 'POST' })
}
