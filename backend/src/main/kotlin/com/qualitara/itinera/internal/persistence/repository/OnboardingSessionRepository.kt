package com.qualitara.itinera.internal.persistence.repository

import com.qualitara.itinera.internal.persistence.model.OnboardingSessionRecord
import com.qualitara.itinera.internal.persistence.model.OnboardingSessionStatus
import com.qualitara.itinera.internal.persistence.model.OnboardingStepKey
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Persists [OnboardingSessionRecord] rows and provides session-level queries.
 *
 * Owns: SQL execution, row mapping, session lifecycle persistence shape.
 * Does not own: workflow transitions, business validation, allowed-actions calculation,
 * or which step a session should move to next.
 *
 * Transaction management is the caller's responsibility; this repository executes
 * the SQL statements it is given without opening or committing transactions.
 */
@Repository
class OnboardingSessionRepository(private val jdbc: NamedParameterJdbcTemplate) {

    fun insert(record: OnboardingSessionRecord) {
        val sql = """
            INSERT INTO onboarding_session (id, current_step, status, created_at, updated_at, completed_at)
            VALUES (:id, :currentStep::onboarding_step_key, :status::onboarding_session_status,
                    :createdAt, :updatedAt, :completedAt)
        """.trimIndent()
        jdbc.update(sql, record.toParams())
    }

    fun findById(id: UUID): OnboardingSessionRecord? {
        val sql = "SELECT * FROM onboarding_session WHERE id = :id"
        return jdbc.query(sql, MapSqlParameterSource("id", id), rowMapper).firstOrNull()
    }

    fun updateCurrentStepAndStatus(id: UUID, currentStep: OnboardingStepKey, status: OnboardingSessionStatus) {
        val sql = """
            UPDATE onboarding_session
            SET current_step = :currentStep::onboarding_step_key,
                status       = :status::onboarding_session_status,
                updated_at   = :updatedAt
            WHERE id = :id
        """.trimIndent()
        jdbc.update(sql, MapSqlParameterSource()
            .addValue("id", id)
            .addValue("currentStep", currentStep.name)
            .addValue("status", status.name)
            .addValue("updatedAt", OffsetDateTime.now()))
    }

    fun markCompleted(id: UUID, completedAt: OffsetDateTime) {
        val sql = """
            UPDATE onboarding_session
            SET status       = 'LIVE'::onboarding_session_status,
                completed_at = :completedAt,
                updated_at   = :completedAt
            WHERE id = :id
        """.trimIndent()
        jdbc.update(sql, MapSqlParameterSource()
            .addValue("id", id)
            .addValue("completedAt", completedAt))
    }

    private fun OnboardingSessionRecord.toParams(): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("id", id)
            .addValue("currentStep", currentStep.name)
            .addValue("status", status.name)
            .addValue("createdAt", createdAt)
            .addValue("updatedAt", updatedAt)
            .addValue("completedAt", completedAt)

    private val rowMapper = RowMapper<OnboardingSessionRecord> { rs, _ -> rs.toRecord() }

    private fun ResultSet.toRecord() = OnboardingSessionRecord(
        id = getObject("id", UUID::class.java),
        currentStep = OnboardingStepKey.valueOf(getString("current_step")),
        status = OnboardingSessionStatus.valueOf(getString("status")),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        updatedAt = getObject("updated_at", OffsetDateTime::class.java),
        completedAt = getObject("completed_at", OffsetDateTime::class.java)
    )
}
