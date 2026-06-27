# Known Limitations

## Delivery Status

Itinera's agreed vertical slice is complete. The backend workflow, persistence model, Provider validation boundary, REST contract, transactional go-live behavior, Docker runtime, and evaluator-facing documentation are implemented and validated.

The project was delivered within a fixed engineering time budget. Delivery favors correctness, architecture, resumability, idempotency, documentation, and engineering discipline over exhaustive feature completeness.

## Deferred UI Integration

The backend exposes `EDIT_DETAILS` in `allowedActions` when workflow policy permits editing from Validation or Review, including retryable `UNAVAILABLE` and `TIMEOUT` states. The REST API accepts the details update, detects credential changes, marks the previous validation `STALE`, and requires validation again.

The submitted frontend implements the minimal wizard required for the exercise. It renders Details when `currentStep = DETAILS`, retry controls in Validation, and go-live controls in Review. It does not render navigation from Validation or Review back to the details form.

As a result:

- retrying the current credentials after `UNAVAILABLE` or `TIMEOUT` is available;
- returning to Details after `INVALID` is available;
- reload/resume works in retryable states;
- changing credentials directly from Validation or Review is not available through the UI;
- the corresponding backend workflow and REST behavior are complete and covered by tests.

This UI integration gap was identified during final evaluator acceptance testing. Wiring the existing `EDIT_DETAILS` action into the frontend was intentionally deferred at project closure to respect the agreed engineering time budget. It is not an architectural limitation and does not require backend changes.

## Closure

No further implementation work is included in the delivered scope. Prospective product, infrastructure, and testing investments are listed in [FUTURE_FORWARDS.md](FUTURE_FORWARDS.md).
