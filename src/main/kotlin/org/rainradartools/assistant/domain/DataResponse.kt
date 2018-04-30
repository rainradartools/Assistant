package org.rainradartools.assistant.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataResponse(
        @JsonProperty("radar_id")
        val radarId: String,

        @JsonProperty("use_cache")
        val useCache: Boolean,

        val size: String,

        @JsonProperty("animation_type")
        val animationType: String,

        @JsonProperty("target_bucket")
        val targetBucket: String,

        @JsonProperty("target_key")
        val targetKey: String,

        @JsonProperty("source_bucket")
        val sourceBucket: String,

        @JsonProperty("layer_keys")
        val layerKeys: List<String>,

        @JsonProperty("freeze_keys")
        val freezeKeys: List<String>,

        @JsonProperty("rain_keys")
        val rainKeys: List<String>,

        val first: String,
        val last: String
)