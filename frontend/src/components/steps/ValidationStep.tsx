import type { ValidationStatus, AllowedAction } from '../../types/onboarding'

interface ValidationStepProps {
  validationStatus: ValidationStatus
  allowedActions: AllowedAction[]
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

export function ValidationStep({ validationStatus, allowedActions }: ValidationStepProps) {
  const canValidate =
    allowedActions.includes('START_VALIDATION') ||
    allowedActions.includes('RETRY_VALIDATION')

  const isPending = validationStatus === 'PENDING'
  const isDone = validationStatus === 'VALID' || validationStatus === 'PARTIAL'

  return (
    <div className="step-panel">
      <h2>Validate Integration</h2>

      <div className={`status-panel status-${validationStatus.toLowerCase()}`}>
        <p className="status-message">{STATUS_MESSAGES[validationStatus]}</p>
      </div>

      {!isDone && (
        <button type="button" disabled={!canValidate || isPending}>
          {isPending ? 'Validating…' : 'Validate Credentials'}
        </button>
      )}
    </div>
  )
}
