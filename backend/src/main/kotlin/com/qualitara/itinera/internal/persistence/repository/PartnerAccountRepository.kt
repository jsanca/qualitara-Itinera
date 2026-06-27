package com.qualitara.itinera.internal.persistence.repository

import com.qualitara.itinera.internal.persistence.model.PartnerAccountRecord
import com.qualitara.itinera.internal.persistence.model.PartnerAccountStatus
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Persists [PartnerAccountRecord] rows created at go-live time.
 *
 * Owns: SQL execution, row mapping, account persistence shape.
 * Does not own: go-live transaction orchestration, idempotency policy, or
 * whether a session is eligible to go live.
 *
 * [findBySessionId] supports idempotent go-live: callers check for an
 * existing account before inserting. The repository provides no locking.
 */
@Repository
class PartnerAccountRepository(private val jdbc: NamedParameterJdbcTemplate) {

    fun insert(record: PartnerAccountRecord) {
        val sql = """
            INSERT INTO partner_account (id, session_id, company_name, status, went_live_at, created_at)
            VALUES (:id, :sessionId, :companyName, :status::partner_account_status, :wentLiveAt, :createdAt)
        """.trimIndent()
        jdbc.update(sql, record.toParams())
    }

    fun findById(id: UUID): PartnerAccountRecord? {
        val sql = "SELECT * FROM partner_account WHERE id = :id"
        return jdbc.query(sql, MapSqlParameterSource("id", id), rowMapper).firstOrNull()
    }

    fun findBySessionId(sessionId: UUID): PartnerAccountRecord? {
        val sql = "SELECT * FROM partner_account WHERE session_id = :sessionId"
        return jdbc.query(sql, MapSqlParameterSource("sessionId", sessionId), rowMapper).firstOrNull()
    }

    private fun PartnerAccountRecord.toParams(): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("id", id)
            .addValue("sessionId", sessionId)
            .addValue("companyName", companyName)
            .addValue("status", status.name)
            .addValue("wentLiveAt", wentLiveAt)
            .addValue("createdAt", createdAt)

    private val rowMapper = RowMapper<PartnerAccountRecord> { rs, _ -> rs.toRecord() }

    private fun ResultSet.toRecord() = PartnerAccountRecord(
        id = getObject("id", UUID::class.java),
        sessionId = getObject("session_id", UUID::class.java),
        companyName = getString("company_name"),
        status = PartnerAccountStatus.valueOf(getString("status")),
        wentLiveAt = getObject("went_live_at", OffsetDateTime::class.java),
        createdAt = getObject("created_at", OffsetDateTime::class.java)
    )
}
