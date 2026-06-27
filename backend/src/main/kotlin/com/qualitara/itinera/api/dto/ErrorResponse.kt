package com.qualitara.itinera.api.dto

/** Uniform error shape returned for all 4xx responses. */
data class ErrorResponse(
    val code: String,
    val message: String
)
