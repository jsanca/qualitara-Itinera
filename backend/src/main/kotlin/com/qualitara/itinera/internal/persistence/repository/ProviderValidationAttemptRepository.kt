package com.qualitara.itinera.internal.persistence.repository

import com.qualitara.itinera.internal.persistence.json.JsonbPayloadMapper
import com.qualitara.itinera.internal.persistence.model.ProviderValidationAttemptRecord
import com.qualitara.itinera.internal.persistence.model.ProviderValidationOutcome
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Persists [ProviderValidationAttemptRecord] audit entries for every validation call.
 *
 * Owns: SQL execution, row mapping, attempt numbering.
 * Does not own: Provider call logic, retry policy, or which attempt is "current"
 * for workflow decisions.
 *
 * [nextAttemptNumber] is the single source of truth for the next attempt index
 * within a session and is used to enforce idempotent, auditable validation attempts.
 */
@Repository
class ProviderValidationAttemptRepository(
    private val jdbc: NamedParameterJdbcTemplate,
    private val payloadMapper: JsonbPayloadMapper
) {

    fun insert(record: ProviderValidationAttemptRecord) {
        val sql = """
            INSERT INTO provider_validation_attempt
                (id, session_id, attempt_number, account_id, request_fingerprint,
                 outcome, response_payload, error_message, started_at, completed_at)
            VALUES
                (:id, :sessionId, :attemptNumber, :accountId, :requestFingerprint,
                 :outcome::provider_validation_outcome, :responsePayload, :errorMessage,
                 :startedAt, :completedAt)
        """.trimIndent()
        jdbc.update(sql, record.toParams())
    }

    fun findBySessionIdOrderByAttemptNumber(sessionId: UUID): List<ProviderValidationAttemptRecord> {
        val sql = """
            SELECT * FROM provider_validation_attempt
            WHERE session_id = :sessionId
            ORDER BY attempt_number ASC
        """.trimIndent()
        return jdbc.query(sql, MapSqlParameterSource("sessionId", sessionId), rowMapper)
    }

    fun nextAttemptNumber(sessionId: UUID): Int {
        val sql = """
            SELECT COALESCE(MAX(attempt_number), 0) + 1
            FROM provider_validation_attempt
            WHERE session_id = :sessionId
        """.trimIndent()
        return jdbc.queryForObject(sql, MapSqlParameterSource("sessionId", sessionId), Int::class.java) ?: 1
    }

    private fun ProviderValidationAttemptRecord.toParams(): MapSqlParameterSource {
        return MapSqlParameterSource()
            .addValue("id", id)
            .addValue("sessionId", sessionId)
            .addValue("attemptNumber", attemptNumber)
            .addValue("accountId", accountId)
            .addValue("requestFingerprint", requestFingerprint)
            .addValue("outcome", outcome.name)
            .addValue("responsePayload", payloadMapper.wrapJsonStringOrNull(responsePayloadJson))
            .addValue("errorMessage", errorMessage)
            .addValue("startedAt", startedAt)
            .addValue("completedAt", completedAt)
    }

    private val rowMapper = RowMapper<ProviderValidationAttemptRecord> { rs, _ -> rs.toRecord() }

    private fun ResultSet.toRecord() = ProviderValidationAttemptRecord(
        id = getObject("id", UUID::class.java),
        sessionId = getObject("session_id", UUID::class.java),
        attemptNumber = getInt("attempt_number"),
        accountId = getString("account_id"),
        requestFingerprint = getString("request_fingerprint"),
        outcome = ProviderValidationOutcome.valueOf(getString("outcome")),
        responsePayloadJson = payloadMapper.extractJsonbString(this, "response_payload"),
        errorMessage = getString("error_message"),
        startedAt = getObject("started_at", OffsetDateTime::class.java),
        completedAt = getObject("completed_at", OffsetDateTime::class.java)
    )
}
