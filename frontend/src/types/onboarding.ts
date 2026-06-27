export type OnboardingStepKey = 'DETAILS' | 'VALIDATION' | 'REVIEW' | 'COMPLETE'

export type OnboardingSessionStatus = 'DRAFT' | 'LIVE'

export type OnboardingStepStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'BLOCKED'

export type ProviderValidationOutcome = 'VALID' | 'PARTIAL' | 'INVALID' | 'UNAVAILABLE' | 'TIMEOUT'

export type AllowedAction =
  | 'submit_details'
  | 'retry_validation'
  | 'edit_details'
  | 'go_live'

export interface DetailsPayload {
  companyName: string
  apiKey: string
  apiKeyPresent: boolean
  apiKeyMasked: string
}

export interface ValidationPayload {
  outcome: ProviderValidationOutcome
  accountId: string
  message?: string
}

export interface ReviewPayload {
  accountId: string
  companyName: string
  itemCount?: number
}

export interface OnboardingSessionResponse {
  sessionId: string
  currentStep: OnboardingStepKey
  status: OnboardingSessionStatus
  allowedActions: AllowedAction[]
  details?: DetailsPayload
  validation?: ValidationPayload
}

export interface ProviderItem {
  id: string
  name: string
  type: string
}
