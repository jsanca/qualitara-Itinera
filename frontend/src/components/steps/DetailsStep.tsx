import { useState } from 'react'
import type { DetailsSummary, AllowedAction } from '../../types/onboarding'

interface DetailsStepProps {
  details: DetailsSummary | null
  allowedActions: AllowedAction[]
  validationReason?: string | null
  onSubmit: (companyName: string, accountId: string, apiKey: string) => void
  isLoading: boolean
}

export function DetailsStep({ details, allowedActions, validationReason, onSubmit, isLoading }: DetailsStepProps) {
  const [companyName, setCompanyName] = useState(details?.companyName ?? '')
  const [accountId, setAccountId] = useState(details?.accountId ?? '')
  const [apiKey, setApiKey] = useState('')

  const canSubmit = allowedActions.includes('SUBMIT_DETAILS') || allowedActions.includes('EDIT_DETAILS')

  function handleSubmit() {
    if (!canSubmit || isLoading) return
    onSubmit(companyName, accountId, apiKey)
    setApiKey('')
  }

  return (
    <div className="step-panel">
      <h2>Partner Details</h2>
      <p>Enter your Provider credentials to begin.</p>

      {validationReason && (
        <div className="status-panel status-invalid" style={{ marginBottom: '1rem' }}>
          <p className="status-message">{validationReason}</p>
        </div>
      )}

      <form className="details-form" onSubmit={(e) => { e.preventDefault(); handleSubmit() }}>
        <label className="form-field">
          <span>Company Name</span>
          <input
            type="text"
            value={companyName}
            onChange={(e) => setCompanyName(e.target.value)}
            placeholder="Acme Logistics"
            disabled={isLoading}
          />
        </label>

        <label className="form-field">
          <span>Account ID</span>
          <input
            type="text"
            value={accountId}
            onChange={(e) => setAccountId(e.target.value)}
            placeholder="acct-12345"
            disabled={isLoading}
          />
        </label>

        <label className="form-field">
          <span>API Key</span>
          <input
            type="password"
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            placeholder="provider-secret-value"
            disabled={isLoading}
          />
        </label>

        {details?.apiKeyPresent && (
          <p className="field-note">
            API key stored: {details.apiKeyMasked ?? '(masked)'}
          </p>
        )}

        <button type="submit" disabled={!canSubmit || isLoading}>
          {isLoading ? 'Saving…' : allowedActions.includes('SUBMIT_DETAILS') ? 'Submit Details' : 'Update Details'}
        </button>
      </form>
    </div>
  )
}
