package org.rainradartools.assistant.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class DataResponseEncryption(
        @JsonProperty("animation_request")
        val animationRequest: List<String>,
        val radarId: String,
        val first: String,
        val last: String
)