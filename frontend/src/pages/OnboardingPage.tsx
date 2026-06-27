import { useState } from 'react'
import { StepShell } from '../components/StepShell'
import { DetailsStep } from '../components/steps/DetailsStep'
import { ValidationStep } from '../components/steps/ValidationStep'
import { ReviewStep } from '../components/steps/ReviewStep'
import type { OnboardingSessionResponse, OnboardingStepKey } from '../types/onboarding'

const MOCK_SESSIONS: Record<OnboardingStepKey, OnboardingSessionResponse> = {
  DETAILS: {
    sessionId: '00000000-0000-0000-0000-000000000001',
    currentStep: 'DETAILS',
    sessionStatus: 'DRAFT',
    validationStatus: 'NOT_STARTED',
    details: null,
    validation: { status: 'NOT_STARTED', items: [], warnings: [], reason: null },
    allowedActions: ['SUBMIT_DETAILS'],
  },
  VALIDATION: {
    sessionId: '00000000-0000-0000-0000-000000000001',
    currentStep: 'VALIDATION',
    sessionStatus: 'DRAFT',
    validationStatus: 'NOT_STARTED',
    details: {
      companyName: 'Acme Logistics',
      accountId: 'acct-12345',
      apiKeyPresent: true,
      apiKeyMasked: '********wxyz',
    },
    validation: { status: 'NOT_STARTED', items: [], warnings: [], reason: null },
    allowedActions: ['EDIT_DETAILS', 'START_VALIDATION'],
  },
  REVIEW: {
    sessionId: '00000000-0000-0000-0000-000000000001',
    currentStep: 'REVIEW',
    sessionStatus: 'DRAFT',
    validationStatus: 'PARTIAL',
    details: {
      companyName: 'Acme Logistics',
      accountId: 'acct-12345',
      apiKeyPresent: true,
      apiKeyMasked: '********wxyz',
    },
    validation: {
      status: 'PARTIAL',
      items: [
        { externalId: 'feed-42', name: 'Primary shipment feed', status: 'AVAILABLE' },
        { externalId: 'feed-99', name: 'Secondary feed', status: 'PAUSED' },
      ],
      warnings: ['One optional feed is unavailable.'],
      reason: null,
    },
    allowedActions: ['EDIT_DETAILS', 'GO_LIVE'],
  },
  COMPLETE: {
    sessionId: '00000000-0000-0000-0000-000000000001',
    currentStep: 'COMPLETE',
    sessionStatus: 'LIVE',
    validationStatus: 'PARTIAL',
    details: {
      companyName: 'Acme Logistics',
      accountId: 'acct-12345',
      apiKeyPresent: true,
      apiKeyMasked: '********wxyz',
    },
    validation: {
      status: 'PARTIAL',
      items: [
        { externalId: 'feed-42', name: 'Primary shipment feed', status: 'AVAILABLE' },
      ],
      warnings: [],
      reason: null,
    },
    allowedActions: [],
  },
}

export function OnboardingPage() {
  const [activeStep, setActiveStep] = useState<OnboardingStepKey>('DETAILS')
  const session = MOCK_SESSIONS[activeStep]

  return (
    <StepShell currentStep={session.currentStep}>
      <div className="demo-controls">
        <span className="demo-label">Demo step:</span>
        {(['DETAILS', 'VALIDATION', 'REVIEW', 'COMPLETE'] as OnboardingStepKey[]).map((step) => (
          <button
            key={step}
            className={`demo-btn ${activeStep === step ? 'active' : ''}`}
            onClick={() => setActiveStep(step)}
            type="button"
          >
            {step}
          </button>
        ))}
      </div>

      {session.currentStep === 'DETAILS' && (
        <DetailsStep
          details={session.details}
          allowedActions={session.allowedActions}
        />
      )}

      {session.currentStep === 'VALIDATION' && (
        <ValidationStep
          validationStatus={session.validationStatus}
          allowedActions={session.allowedActions}
        />
      )}

      {session.currentStep === 'REVIEW' && (
        <ReviewStep
          validation={session.validation}
          allowedActions={session.allowedActions}
        />
      )}

      {session.currentStep === 'COMPLETE' && (
        <div className="step-panel">
          <h2>Onboarding Complete</h2>
          <p>Your account is live. Welcome, {session.details?.companyName}.</p>
        </div>
      )}
    </StepShell>
  )
}
