package org.rainradartools.assistant.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProxyRequest(
        val body: String?,
        val httpMethod: String
)