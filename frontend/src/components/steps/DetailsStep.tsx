import { useState } from 'react'
import type { DetailsSummary, AllowedAction } from '../../types/onboarding'

interface DetailsStepProps {
  details: DetailsSummary | null
  allowedActions: AllowedAction[]
}

export function DetailsStep({ details, allowedActions }: DetailsStepProps) {
  const [companyName, setCompanyName] = useState(details?.companyName ?? '')
  const [accountId, setAccountId] = useState(details?.accountId ?? '')
  const [apiKey, setApiKey] = useState('')

  const canSubmit = allowedActions.includes('SUBMIT_DETAILS') || allowedActions.includes('EDIT_DETAILS')

  return (
    <div className="step-panel">
      <h2>Partner Details</h2>
      <p>Enter your Provider credentials to begin.</p>

      <form className="details-form">
        <label className="form-field">
          <span>Company Name</span>
          <input
            type="text"
            value={companyName}
            onChange={(e) => setCompanyName(e.target.value)}
            placeholder="Acme Logistics"
          />
        </label>

        <label className="form-field">
          <span>Account ID</span>
          <input
            type="text"
            value={accountId}
            onChange={(e) => setAccountId(e.target.value)}
            placeholder="acct-12345"
          />
        </label>

        <label className="form-field">
          <span>API Key</span>
          <input
            type="password"
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            placeholder="provider-secret-value"
          />
        </label>

        {details?.apiKeyPresent && (
          <p className="field-note">
            API key stored: {details.apiKeyMasked ?? '(masked)'}
          </p>
        )}

        <button type="button" disabled={!canSubmit}>
          {allowedActions.includes('SUBMIT_DETAILS') ? 'Submit Details' : 'Update Details'}
        </button>
      </form>
    </div>
  )
}
