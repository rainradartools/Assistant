package org.rainradartools.assistant.domain

data class ProxyResponse(
        var body: String,
        val statusCode: Int,
        val headers: Map<String, String>
)
