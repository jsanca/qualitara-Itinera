package com.qualitara.itinera.internal.persistence.repository

import com.qualitara.itinera.internal.persistence.json.JsonbPayloadMapper
import com.qualitara.itinera.internal.persistence.model.OnboardingStepKey
import com.qualitara.itinera.internal.persistence.model.OnboardingStepStateRecord
import com.qualitara.itinera.internal.persistence.model.OnboardingStepStatus
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Persists [OnboardingStepStateRecord] rows for each step of an onboarding session.
 *
 * Owns: SQL execution, row mapping, upsert logic for idempotent step-state writes.
 * Does not own: which step is current, what a valid payload looks like,
 * or step-level transition rules.
 *
 * [upsert] implements the persistence-level idempotency contract using
 * `ON CONFLICT (session_id, step_key) DO UPDATE`. Callers must supply
 * the correct payload version; this repository does not interpret it.
 */
@Repository
class OnboardingStepStateRepository(
    private val jdbc: NamedParameterJdbcTemplate,
    private val payloadMapper: JsonbPayloadMapper
) {

    fun upsert(record: OnboardingStepStateRecord) {
        val sql = """
            INSERT INTO onboarding_step_state
                (id, session_id, step_key, status, payload, payload_version, created_at, updated_at, completed_at)
            VALUES
                (:id, :sessionId, :stepKey::onboarding_step_key, :status::onboarding_step_status,
                 :payload, :payloadVersion, :createdAt, :updatedAt, :completedAt)
            ON CONFLICT (session_id, step_key) DO UPDATE SET
                status          = EXCLUDED.status,
                payload         = EXCLUDED.payload,
                payload_version = EXCLUDED.payload_version,
                updated_at      = EXCLUDED.updated_at,
                completed_at    = EXCLUDED.completed_at
        """.trimIndent()
        jdbc.update(sql, record.toParams())
    }

    fun findBySessionIdAndStepKey(sessionId: UUID, stepKey: OnboardingStepKey): OnboardingStepStateRecord? {
        val sql = """
            SELECT * FROM onboarding_step_state
            WHERE session_id = :sessionId AND step_key = :stepKey::onboarding_step_key
        """.trimIndent()
        return jdbc.query(sql, MapSqlParameterSource()
            .addValue("sessionId", sessionId)
            .addValue("stepKey", stepKey.name), rowMapper).firstOrNull()
    }

    fun findAllBySessionId(sessionId: UUID): List<OnboardingStepStateRecord> {
        val sql = "SELECT * FROM onboarding_step_state WHERE session_id = :sessionId"
        return jdbc.query(sql, MapSqlParameterSource("sessionId", sessionId), rowMapper)
    }

    private fun OnboardingStepStateRecord.toParams(): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("id", id)
            .addValue("sessionId", sessionId)
            .addValue("stepKey", stepKey.name)
            .addValue("status", status.name)
            .addValue("payload", payloadMapper.wrapJsonString(payloadJson))
            .addValue("payloadVersion", payloadVersion)
            .addValue("createdAt", createdAt)
            .addValue("updatedAt", updatedAt)
            .addValue("completedAt", completedAt)

    private val rowMapper = RowMapper<OnboardingStepStateRecord> { rs, _ -> rs.toRecord() }

    private fun ResultSet.toRecord() = OnboardingStepStateRecord(
        id = getObject("id", UUID::class.java),
        sessionId = getObject("session_id", UUID::class.java),
        stepKey = OnboardingStepKey.valueOf(getString("step_key")),
        status = OnboardingStepStatus.valueOf(getString("status")),
        payloadJson = payloadMapper.extractJsonbString(this, "payload") ?: "{}",
        payloadVersion = getInt("payload_version"),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        updatedAt = getObject("updated_at", OffsetDateTime::class.java),
        completedAt = getObject("completed_at", OffsetDateTime::class.java)
    )
}
