package com.qualitara.itinera.workflow.payload

/** A single item discovered during Provider validation (e.g., a feed, integration, or resource). */
data class ProviderItem(
    val externalId: String,
    val name: String,
    val status: String
)
