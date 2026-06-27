export type OnboardingStepKey = 'DETAILS' | 'VALIDATION' | 'REVIEW' | 'COMPLETE'

export type OnboardingSessionStatus = 'DRAFT' | 'LIVE'

export type ValidationStatus =
  | 'NOT_STARTED'
  | 'PENDING'
  | 'VALID'
  | 'PARTIAL'
  | 'INVALID'
  | 'UNAVAILABLE'
  | 'TIMEOUT'
  | 'STALE'

export type AllowedAction =
  | 'SUBMIT_DETAILS'
  | 'EDIT_DETAILS'
  | 'START_VALIDATION'
  | 'RETRY_VALIDATION'
  | 'GO_TO_REVIEW'
  | 'GO_LIVE'

export interface DetailsSummary {
  companyName: string
  accountId: string
  apiKeyPresent: boolean
  apiKeyMasked: string | null
}

export interface ProviderItem {
  externalId: string
  name: string
  status: string
}

export interface ValidationSummary {
  status: ValidationStatus
  items: ProviderItem[]
  warnings: string[]
  reason: string | null
}

export interface OnboardingSessionResponse {
  sessionId: string
  currentStep: OnboardingStepKey
  sessionStatus: OnboardingSessionStatus
  validationStatus: ValidationStatus
  details: DetailsSummary | null
  validation: ValidationSummary
  allowedActions: AllowedAction[]
}

export interface SubmitDetailsRequest {
  companyName: string
  accountId: string
  apiKey: string
}
