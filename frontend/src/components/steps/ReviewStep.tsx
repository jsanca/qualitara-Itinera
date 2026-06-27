import type { ValidationSummary, AllowedAction, ProviderItem } from '../../types/onboarding'

interface ReviewStepProps {
  validation: ValidationSummary
  allowedActions: AllowedAction[]
}

export function ReviewStep({ validation, allowedActions }: ReviewStepProps) {
  const canGoLive = allowedActions.includes('GO_LIVE')
  const hasWarnings = validation.warnings.length > 0

  return (
    <div className="step-panel">
      <h2>Review &amp; Go Live</h2>

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

      <button type="button" disabled={!canGoLive}>
        Go Live
      </button>
    </div>
  )
}
