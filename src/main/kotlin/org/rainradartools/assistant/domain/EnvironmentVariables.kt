package org.rainradartools.assistant.domain

data class EnvironmentVariablesDtc (
        val sourceBucket: String,
        val targetBucket: String,
        val configBucket: String,
        val radarFactsKey: String,
        val encryption: String,
        val kmsKeyId: String,
        val maxTimesliceMins: String
)
