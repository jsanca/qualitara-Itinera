import { useEffect, useState } from 'react'
import { StepShell } from '../components/StepShell'
import { DetailsStep } from '../components/steps/DetailsStep'
import { ValidationStep } from '../components/steps/ValidationStep'
import { ReviewStep } from '../components/steps/ReviewStep'
import {
  createSession,
  getSession,
  submitDetails,
  triggerValidation,
  goLive,
} from '../api/onboardingApi'
import type { OnboardingSessionResponse } from '../types/onboarding'

const SESSION_KEY = 'itinera.sessionId'

export function OnboardingPage() {
  const [session, setSession] = useState<OnboardingSessionResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    bootstrap()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function bootstrap() {
    setLoading(true)
    setError(null)
    try {
      const stored = localStorage.getItem(SESSION_KEY)
      let s: OnboardingSessionResponse
      if (stored) {
        try {
          s = await getSession(stored)
        } catch (e: unknown) {
          const err = e as { code?: string }
          if (err?.code === 'SESSION_NOT_FOUND') {
            localStorage.removeItem(SESSION_KEY)
            s = await createSession()
          } else {
            throw e
          }
        }
      } else {
        s = await createSession()
      }
      localStorage.setItem(SESSION_KEY, s.sessionId)
      setSession(s)
    } catch (e: unknown) {
      const err = e as { message?: string }
      setError(err?.message ?? 'Failed to load session.')
    } finally {
      setLoading(false)
    }
  }

  async function handleDetailsSubmit(companyName: string, accountId: string, apiKey: string) {
    if (!session) return
    setLoading(true)
    setError(null)
    try {
      setSession(await submitDetails(session.sessionId, { companyName, accountId, apiKey }))
    } catch (e: unknown) {
      setError((e as { message?: string })?.message ?? 'Failed to submit details.')
    } finally {
      setLoading(false)
    }
  }

  async function handleValidate(apiKey: string) {
    if (!session) return
    setLoading(true)
    setError(null)
    try {
      setSession(await triggerValidation(session.sessionId, apiKey))
    } catch (e: unknown) {
      setError((e as { message?: string })?.message ?? 'Validation failed.')
    } finally {
      setLoading(false)
    }
  }

  async function handleGoLive() {
    if (!session) return
    setLoading(true)
    setError(null)
    try {
      setSession(await goLive(session.sessionId))
    } catch (e: unknown) {
      setError((e as { message?: string })?.message ?? 'Go live failed.')
    } finally {
      setLoading(false)
    }
  }

  if (loading && !session) {
    return (
      <div className="step-shell">
        <p style={{ color: '#666' }}>Loading session…</p>
      </div>
    )
  }

  if (!session) {
    return (
      <div className="step-shell">
        <p style={{ color: '#c0392b' }}>Failed to initialize session.</p>
        <button onClick={bootstrap} style={{ marginTop: '1rem' }}>Retry</button>
      </div>
    )
  }

  return (
    <StepShell currentStep={session.currentStep}>
      {error && (
        <div className="error-banner">
          <span>{error}</span>
          <button
            onClick={() => setError(null)}
            className="error-dismiss"
            aria-label="Dismiss error"
          >
            ×
          </button>
        </div>
      )}

      {session.currentStep === 'DETAILS' && (
        <DetailsStep
          details={session.details}
          allowedActions={session.allowedActions}
          validationReason={
            session.validationStatus === 'INVALID' ? session.validation.reason : null
          }
          onSubmit={handleDetailsSubmit}
          isLoading={loading}
        />
      )}

      {session.currentStep === 'VALIDATION' && (
        <ValidationStep
          validationStatus={session.validationStatus}
          validation={session.validation}
          allowedActions={session.allowedActions}
          onValidate={handleValidate}
          isLoading={loading}
        />
      )}

      {session.currentStep === 'REVIEW' && (
        <ReviewStep
          details={session.details}
          validation={session.validation}
          allowedActions={session.allowedActions}
          onGoLive={handleGoLive}
          isLoading={loading}
        />
      )}

      {session.currentStep === 'COMPLETE' && (
        <div className="step-panel">
          <h2>Onboarding Complete</h2>
          <p>Your partner account is live. Welcome, {session.details?.companyName}.</p>
          <button
            style={{ marginTop: '1rem', background: '#888' }}
            onClick={() => {
              localStorage.removeItem(SESSION_KEY)
              setSession(null)
              bootstrap()
            }}
          >
            Start New Session
          </button>
        </div>
      )}
    </StepShell>
  )
}
