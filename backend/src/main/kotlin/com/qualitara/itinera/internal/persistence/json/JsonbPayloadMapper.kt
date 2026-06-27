package com.qualitara.itinera.internal.persistence.json

import com.fasterxml.jackson.databind.ObjectMapper
import org.postgresql.util.PGobject
import org.springframework.stereotype.Component
import java.sql.ResultSet

/**
 * Serializes Kotlin objects to PostgreSQL JSONB and deserializes JSONB back to typed objects.
 *
 * This mapper handles only structural JSON conversion. It does not:
 * - validate business meaning of payloads
 * - enforce payload schema compatibility
 * - decide which payload version is acceptable
 * - manage partial results or error reporting
 *
 * All Jackson configuration (serialization options, null handling) is
 * delegated to the injected [ObjectMapper] bean.
 */
@Component
class JsonbPayloadMapper(private val objectMapper: ObjectMapper) {

    fun toJsonString(payload: Any): String = objectMapper.writeValueAsString(payload)

    fun toPGobject(payload: Any): PGobject = PGobject().apply {
        type = "jsonb"
        value = toJsonString(payload)
    }

    fun toPGobjectOrNull(payload: Any?): PGobject? {
        if (payload == null) return null
        return toPGobject(payload)
    }

    /** Wraps an already-serialized JSON string in a JSONB PGobject without re-encoding it. */
    fun wrapJsonString(json: String): PGobject = PGobject().apply {
        type = "jsonb"
        value = json
    }

    fun wrapJsonStringOrNull(json: String?): PGobject? = json?.let { wrapJsonString(it) }

    fun <T> fromJsonString(json: String, type: Class<T>): T = objectMapper.readValue(json, type)

    fun extractJsonbString(rs: ResultSet, column: String): String? {
        val obj = rs.getObject(column) ?: return null
        return when (obj) {
            is PGobject -> obj.value
            is String -> obj
            else -> obj.toString()
        }
    }
}
