/**
 * Repository classes that own SQL execution, row mapping, and persistence shape
 * for onboarding sessions, step states, partner accounts, and validation attempts.
 *
 * ## What repositories own
 * - SQL statement execution via [NamedParameterJdbcTemplate]
 * - Row-to-record mapping
 * - Persistence-level idempotency (e.g., upserts via ON CONFLICT)
 * - Parameter binding for PostgreSQL-specific types (e.g., JSONB, enums)
 *
 * ## What repositories do not own
 * - Workflow transitions and state machine rules
 * - Business validation and transition eligibility
 * - Provider integration and call logic
 * - Transaction boundaries and orchestration
 * - Cache management
 * - Observability and metrics
 *
 * ## Transaction boundary
 * Repositories execute statements within transactions opened by their callers.
 * A repository method is not responsible for committing or rolling back.
 */
@file:Suppress("PACKAGE")
