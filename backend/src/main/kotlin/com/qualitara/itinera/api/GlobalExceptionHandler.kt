package com.qualitara.itinera.api

import com.qualitara.itinera.api.dto.ErrorResponse
import com.qualitara.itinera.workflow.exception.InvalidWorkflowTransitionException
import com.qualitara.itinera.workflow.exception.UnsupportedPayloadVersionException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

private val log = KotlinLogging.logger {}

/**
 * Maps domain exceptions to consistent HTTP error responses.
 *
 * All errors use [ErrorResponse] with a stable [code] field that the frontend
 * can program against and a human-readable [message] safe to display to users.
 * Internal details (session IDs, stack traces) are never included in responses.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(SessionNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleSessionNotFound(e: SessionNotFoundException): ErrorResponse {
        log.warn { "Session not found: ${e.message}" }
        return ErrorResponse("SESSION_NOT_FOUND", "Onboarding session was not found.")
    }

    @ExceptionHandler(InvalidWorkflowTransitionException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleInvalidTransition(e: InvalidWorkflowTransitionException): ErrorResponse {
        log.warn { "Invalid workflow transition: ${e.message}" }
        return ErrorResponse("INVALID_TRANSITION", "The requested action is not valid for the current session state.")
    }

    @ExceptionHandler(UnsupportedPayloadVersionException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleUnsupportedVersion(e: UnsupportedPayloadVersionException): ErrorResponse {
        log.warn { "Unsupported payload version: ${e.message}" }
        return ErrorResponse("UNSUPPORTED_PAYLOAD_VERSION", e.message ?: "Payload version is not supported.")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationFailure(e: MethodArgumentNotValidException): ErrorResponse {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: "Request is missing required fields."
        log.warn { "Request validation failed: $message" }
        return ErrorResponse("MALFORMED_REQUEST", message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleUnreadableMessage(e: HttpMessageNotReadableException): ErrorResponse {
        log.warn { "Malformed request body: ${e.message}" }
        return ErrorResponse("MALFORMED_REQUEST", "Request body is missing or malformed.")
    }
}
