import type {
  OnboardingSessionResponse,
  SubmitDetailsRequest,
} from '../types/onboarding'

export async function createSession(): Promise<{ sessionId: string }> {
  throw new Error('Not implemented yet')
}

export async function getSession(_sessionId: string): Promise<OnboardingSessionResponse> {
  throw new Error('Not implemented yet')
}

export async function submitDetails(
  _sessionId: string,
  _details: SubmitDetailsRequest
): Promise<OnboardingSessionResponse> {
  throw new Error('Not implemented yet')
}

export async function startValidation(
  _sessionId: string
): Promise<OnboardingSessionResponse> {
  throw new Error('Not implemented yet')
}

export async function goLive(
  _sessionId: string
): Promise<OnboardingSessionResponse> {
  throw new Error('Not implemented yet')
}
