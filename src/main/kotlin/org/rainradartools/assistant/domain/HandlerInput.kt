package org.rainradartools.assistant.domain

import com.amazonaws.util.NumberUtils
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.Duration
import org.joda.time.Interval
import org.joda.time.Period
import java.text.SimpleDateFormat
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.ISODateTimeFormat
import java.util.*


val FORMAT = "yyyyMMddHHmmm"
val MP4 = "mp4"
val GIF = "gif"

data class HandlerInput(
        @JsonProperty("radar_id")
        val radarId: String,

        @JsonProperty("start_datetime")
        val startDateTime: Long,

        @JsonProperty("end_datetime")
        val endDateTime: Long,

        @JsonProperty("animation_type")
        val animationType: String,

        val size: String
) {
        fun validate(radars: MutableSet<String>,
                     animationTypes: List<String>,
                     maxTimesliceMins: Int,
                     maxImageSize: Int
        ) {

                val dateFormat = SimpleDateFormat(FORMAT)
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val start = dateFormat.parse(startDateTime.toString())
                val end = dateFormat.parse(endDateTime.toString())
                val diffMins = ((end.time - start.time) / 1000) / 60

                if (!radars.contains(radarId)) throw BadRequestException("unknown radar id")
                if (start.after(end)) return throw BadRequestException("start time is later than end time")
                if (!animationTypes.contains(animationType)) throw BadRequestException("unknown animation type")
                if(startDateTime < 201701010000 || endDateTime > 203001010000) throw BadRequestException("bad date range")
                if(diffMins > maxTimesliceMins) throw BadRequestException("timeslice too big")
                if(size.toInt() > maxImageSize) throw BadRequestException("size too big")
        }
}
