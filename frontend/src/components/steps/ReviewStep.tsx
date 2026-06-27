import type { ValidationSummary, AllowedAction, ProviderItem, DetailsSummary } from '../../types/onboarding'

interface ReviewStepProps {
  details: DetailsSummary | null
  validation: ValidationSummary
  allowedActions: AllowedAction[]
  onGoLive: () => void
  isLoading: boolean
}

export function ReviewStep({ details, validation, allowedActions, onGoLive, isLoading }: ReviewStepProps) {
  const canGoLive = allowedActions.includes('GO_LIVE')
  const hasWarnings = validation.warnings.length > 0

  return (
    <div className="step-panel">
      <h2>Review &amp; Go Live</h2>

      {details && (
        <div className="items-section">
          <h3>Partner Details</h3>
          <table className="summary-table">
            <tbody>
              <tr><td>Company</td><td>{details.companyName}</td></tr>
              <tr><td>Account ID</td><td>{details.accountId}</td></tr>
              <tr><td>API Key</td><td>{details.apiKeyMasked ?? '(stored)'}</td></tr>
            </tbody>
          </table>
        </div>
      )}

      <div className="items-section">
        <h3>Discovered Provider Items</h3>
        {validation.items.length === 0 ? (
          <p className="empty-note">No items discovered.</p>
        ) : (
          <ul className="items-list">
            {validation.items.map((item: ProviderItem) => (
              <li key={item.externalId} className="item-row">
                <span className="item-name">{item.name}</span>
                <span className={`item-status status-${item.status.toLowerCase()}`}>
                  {item.status}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {hasWarnings && (
        <div className="warnings-section">
          <h3>Warnings</h3>
          <ul className="warnings-list">
            {validation.warnings.map((w, i) => (
              <li key={i} className="warning-item">{w}</li>
            ))}
          </ul>
        </div>
      )}

      <button type="button" disabled={!canGoLive || isLoading} onClick={onGoLive}>
        {isLoading ? 'Going Live…' : 'Go Live'}
      </button>
    </div>
  )
}
