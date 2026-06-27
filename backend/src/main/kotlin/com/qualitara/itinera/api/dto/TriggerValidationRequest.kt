package com.qualitara.itinera.api.dto

import jakarta.validation.constraints.NotBlank

data class TriggerValidationRequest(
    @field:NotBlank(message = "apiKey is required")
    val apiKey: String
)
