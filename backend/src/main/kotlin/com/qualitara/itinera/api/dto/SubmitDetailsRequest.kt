package com.qualitara.itinera.api.dto

import jakarta.validation.constraints.NotBlank

data class SubmitDetailsRequest(
    @field:NotBlank(message = "companyName is required")
    val companyName: String,
    @field:NotBlank(message = "accountId is required")
    val accountId: String,
    @field:NotBlank(message = "apiKey is required")
    val apiKey: String
)
