package com.darkempire78.opencalculator.stealth.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaUploadResponse(
    val url: String,
    val objectName: String
)
