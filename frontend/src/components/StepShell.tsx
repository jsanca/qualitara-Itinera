import type { OnboardingStepKey } from '../types/onboarding'

const STEPS: OnboardingStepKey[] = ['DETAILS', 'VALIDATION', 'REVIEW']

interface StepShellProps {
  currentStep: OnboardingStepKey
  children: React.ReactNode
}

export function StepShell({ currentStep, children }: StepShellProps) {
  return (
    <div className="step-shell">
      <nav className="step-nav" aria-label="Onboarding progress">
        {STEPS.map((step, index) => (
          <span
            key={step}
            className={`step-label ${step === currentStep ? 'active' : ''} ${
              STEPS.indexOf(currentStep) > index ? 'done' : ''
            }`}
          >
            {step}
          </span>
        ))}
      </nav>
      <main className="step-content">{children}</main>
    </div>
  )
}
