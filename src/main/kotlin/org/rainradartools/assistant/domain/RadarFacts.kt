package org.rainradartools.assistant.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class RadarFacts(
        val radars: List<Radar>
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Radar(
            val id: String,

            @JsonProperty("resolution_mins")
            val resolutionMins: Int,

            @JsonProperty("add_offset_mins")
            val addOffsetMins: Int
    )
}