# Itinera Frontend

React + TypeScript + Vite wizard for the Itinera onboarding workflow. It renders backend-owned `currentStep` and `allowedActions`, stores only the session ID in `localStorage`, and resumes the authoritative session on reload.

## Run

Start PostgreSQL and the backend first, then:

```bash
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Vite proxies `/api` requests to `http://localhost:8080`.

For the Docker-served full stack, run `docker compose up --build` from the repository root and open [http://localhost:3000](http://localhost:3000).

## Build Check

```bash
npm run build
```

No automated frontend test framework is configured yet.

## Structure

- `src/api/onboardingApi.ts` — typed client for all five onboarding endpoints
- `src/components/StepShell.tsx` — workflow progress shell
- `src/components/steps/` — details, validation, and review/go-live screens
- `src/pages/OnboardingPage.tsx` — session bootstrap, resume, and action orchestration
- `src/types/onboarding.ts` — manually mirrored API types

Raw API keys exist only in password-field component state and the active request. They are not placed in `localStorage` or returned by the backend.

See the root [README](../README.md), [architecture](../docs/ARCHITECTURE.md), and [API contract](../docs/API_CONTRACT.md).
