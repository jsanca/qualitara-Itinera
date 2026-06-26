/**
 * JSON/JSONB serialization helpers for step payloads.
 *
 * [JsonbPayloadMapper] bridges Kotlin objects and PostgreSQL JSONB columns.
 * It handles only structural serialization and deserialization — no business
 * validation, no schema migration, and no interpretation of payload contents.
 *
 * [VersionedPayload] is a marker interface for DTOs that carry a `version` field.
 * The version is written at payload creation time and stored alongside the JSONB.
 * It is a persistence-level signal only; callers decide how to interpret it.
 */
@file:Suppress("PACKAGE")
