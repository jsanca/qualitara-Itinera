create type onboarding_step_key as enum (
    'DETAILS',
    'VALIDATION',
    'REVIEW',
    'COMPLETE'
);

create type onboarding_session_status as enum (
    'DRAFT',
    'LIVE'
);

create type onboarding_step_status as enum (
    'NOT_STARTED',
    'IN_PROGRESS',
    'COMPLETED',
    'BLOCKED'
);

create type provider_validation_outcome as enum (
    'VALID',
    'PARTIAL',
    'INVALID',
    'UNAVAILABLE',
    'TIMEOUT'
);

create type partner_account_status as enum (
    'LIVE'
);

create table onboarding_session (
    id           uuid primary key,
    current_step onboarding_step_key not null,
    status       onboarding_session_status not null,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    completed_at timestamptz null
);

create table onboarding_step_state (
    id              uuid primary key,
    session_id      uuid not null references onboarding_session(id) on delete cascade,
    step_key        onboarding_step_key not null,
    status          onboarding_step_status not null,
    payload         jsonb not null,
    payload_version integer not null,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    completed_at    timestamptz null,
    constraint onboarding_step_state_session_step_unique unique (session_id, step_key)
);

create table provider_validation_attempt (
    id                  uuid primary key,
    session_id          uuid not null references onboarding_session(id) on delete cascade,
    attempt_number      integer not null,
    account_id          text not null,
    request_fingerprint text not null,
    outcome             provider_validation_outcome not null,
    response_payload    jsonb null,
    error_message       text null,
    started_at          timestamptz not null default now(),
    completed_at        timestamptz null,
    constraint provider_validation_attempt_session_attempt_unique unique (session_id, attempt_number)
);

create table partner_account (
    id          uuid primary key,
    session_id  uuid not null unique references onboarding_session(id),
    company_name text not null,
    status      partner_account_status not null,
    went_live_at timestamptz not null,
    created_at  timestamptz not null default now()
);
