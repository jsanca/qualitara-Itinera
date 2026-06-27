import { useState } from 'react'
import type { ValidationStatus, ValidationSummary, AllowedAction } from '../../types/onboarding'

interface ValidationStepProps {
  validationStatus: ValidationStatus
  validation: ValidationSummary
  allowedActions: AllowedAction[]
  onValidate: (apiKey: string) => void
  isLoading: boolean
}

const STATUS_MESSAGES: Record<ValidationStatus, string> = {
  NOT_STARTED: 'Validation has not started yet.',
  PENDING: 'Validating your Provider credentials…',
  VALID: 'Credentials are valid.',
  PARTIAL: 'Credentials are valid with warnings.',
  INVALID: 'Credentials were rejected. Please check and try again.',
  UNAVAILABLE: 'Provider is temporarily unavailable. Retry when ready.',
  TIMEOUT: 'Provider request timed out. Please retry.',
  STALE: 'Credentials changed since last validation. Please validate again.',
}

export function ValidationStep({ validationStatus, validation, allowedActions, onValidate, isLoading }: ValidationStepProps) {
  const [apiKey, setApiKey] = useState('')

  const canValidate =
    allowedActions.includes('START_VALIDATION') ||
    allowedActions.includes('RETRY_VALIDATION')

  const isPending = validationStatus === 'PENDING'

  function handleValidate() {
    if (!canValidate || isPending || isLoading) return
    onValidate(apiKey)
  }

  return (
    <div className="step-panel">
      <h2>Validate Integration</h2>

      <div className={`status-panel status-${validationStatus.toLowerCase()}`}>
        <p className="status-message">{STATUS_MESSAGES[validationStatus]}</p>
        {validation.reason && <p className="field-note" style={{ marginTop: '0.5rem' }}>{validation.reason}</p>}
      </div>

      {canValidate && (
        <form className="details-form" onSubmit={(e) => { e.preventDefault(); handleValidate() }}>
          <label className="form-field">
            <span>API Key</span>
            <input
              type="password"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="provider-secret-value"
              disabled={isLoading || isPending}
            />
          </label>
          <button type="submit" disabled={!canValidate || isPending || isLoading}>
            {isLoading ? 'Validating…' : allowedActions.includes('RETRY_VALIDATION') ? 'Retry Validation' : 'Validate Credentials'}
          </button>
        </form>
      )}

      {isPending && !canValidate && (
        <p style={{ color: '#888', marginTop: '1rem' }}>Validation in progress…</p>
      )}
    </div>
  )
}
