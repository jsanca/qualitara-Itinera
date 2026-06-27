package com.qualitara.itinera.api

import java.util.UUID

/** Thrown when a session UUID is valid but no session exists in persistence. Maps to HTTP 404. */
class SessionNotFoundException(sessionId: UUID) : RuntimeException("Session not found: $sessionId")
