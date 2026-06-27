package com.qualitara.itinera.internal.persistence.json

/**
 * Marker interface for step payload DTOs that carry a structural version field.
 *
 * The version field is a persistence-level signal only — it records the shape
 * of the payload at write time. It does not drive automatic schema migration,
 * compatibility checks, or payload evolution logic. Callers are responsible
 * for interpreting the version when reading stored payloads.
 */
interface VersionedPayload {
    val version: Int
}
