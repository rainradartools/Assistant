package org.rainradartools.assistant

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.rainradartools.assistant.core.Helpers
import org.rainradartools.assistant.core.TimeHelpers
import org.rainradartools.assistant.domain.*
import java.io.InputStream
import java.io.OutputStream

val ENCRYPT = "ENCRYPT"


class Handler(val config: ConfigDtc? = null) {

    var s3: AmazonS3
    var kms: AWSKMS
    val mapper = jacksonObjectMapper()
    val radarFactsIndex = mutableMapOf<String, RadarFacts.Radar>()
    val sourceBucket = System.getenv("sourceBucket") ?: config!!.environmentVariables.sourceBucket
    val targetBucket = System.getenv("targetBucket") ?: config!!.environmentVariables.targetBucket
    val configBucket = System.getenv("configBucket") ?: config!!.environmentVariables.configBucket
    val radarFactsKey = System.getenv("radarFactsKey") ?: config!!.environmentVariables.radarFactsKey
    val maxTimesliceMins = System.getenv("maxTimesliceMins") ?: config!!.environmentVariables.maxTimesliceMins
    val encryption = System.getenv("encryption") ?: config!!.environmentVariables.encryption
    val kmsKeyId = System.getenv("kmsKeyId") ?: config!!.environmentVariables.kmsKeyId
    val rainPrefix = "rain"
    val layersPrefix = "layers"
    val rawRadarImageSize = "512x512"
    val layers = listOf<String>("background", "topography", "locations")
    val freezes = listOf<String>("layers/freeze/512x512/freeze_1.png", "layers/freeze/512x512/freeze_2.png")

    init {
        println("environment variables:")

        if (config == null) {
            println("config is null")
        }
        else {
            println("config is not null")
        }

        kms = if (config != null) AWSKMSClientBuilder.standard()
                .withCredentials(ProfileCredentialsProvider(config!!.aws.namedProfile))
                .withRegion(config.aws.region)
                .build() else AWSKMSClientBuilder.defaultClient()

        s3 = if (config != null) AmazonS3ClientBuilder.standard()
                .withCredentials(ProfileCredentialsProvider(config!!.aws.namedProfile))
                .withRegion(config.aws.region)
                .build() else AmazonS3ClientBuilder.standard().build()

        val obj = s3.getObject(configBucket, radarFactsKey)
        val radarFacts = mapper.readValue<RadarFacts>(Helpers.convertStreamToString(obj.objectContent))

        radarFacts.radars.forEach {
            radar -> radarFactsIndex.put(radar.id, radar)
        }
    }

    private fun listObjectsBetweenTimestamps(radarId: String, startTime: Long, endTime: Long): List<String> {
        val keysList = mutableListOf<String>()

        val prefix = "$rainPrefix/${radarFactsIndex[radarId]?.id}/$rawRadarImageSize/${TimeHelpers.commonTimestringBase(startTime, endTime)}"

        var objectListing = s3.listObjects(sourceBucket, prefix)

        println(objectListing.isTruncated())

        while(true) {

            val objectSummaries = objectListing.objectSummaries

            objectSummaries.forEach {
                val k = it.key.split("/").last().split(".png").first().toLong()

                if (k >= startTime && k <= endTime) {
                    keysList.add(it.key)
                }
            }

            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing)
            } else {
                break
            }
        }

        return keysList
    }

    private fun listLayerKeys(radarId: String): List<String> {
        val list = mutableListOf<String>()

        layers.forEach {
            list.add("$layersPrefix/$radarId/$rawRadarImageSize/$it.png")
        }
        return list
    }


    fun handler(input: InputStream, output: OutputStream): Unit {
        println("starting handler")
        val eventString = Helpers.convertStreamToString(input)
        println(eventString)

        if (eventString == "{}") {
            println("empty request received")
            return mapper.writeValue(output, HandlerEmpty(nothing = ""))
        }

        val timed = Timed(ENCRYPT)
        var rainKeysList: List<String>? = null
        var first = ""
        var last = ""

        val proxyRequest = mapper.readValue<ProxyRequest>(eventString)

        if (proxyRequest.httpMethod == "OPTIONS") {
            mapper.writeValue(output, ProxyResponse(
                    body = "",
                    statusCode = 200,
                    headers = mapOf(
                            Pair("Access-Control-Allow-Origin", "*"),
                            Pair("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, X-Amz-Security-Token")
                    )))
            return
        }


        if (proxyRequest.body != null ) {
            val event = mapper.readValue<HandlerInput>(proxyRequest.body)

            try {
                event.validate(radarFactsIndex.keys, listOf(MP4, GIF), maxTimesliceMins.toInt(), rawRadarImageSize.split("x").first().toInt())
            } catch (e: BadRequestException) {

                mapper.writeValue(output, ProxyResponse(
                        body = "{\"error\": \"${e.message}\"}",
                        statusCode = 400,
                        headers = mapOf(Pair("Content-Type", "application/json"))))
                return
            }

            rainKeysList = listObjectsBetweenTimestamps(event.radarId, event.startDateTime, event.endDateTime)
            first = rainKeysList.first().split("/").last().split(".png").first()
            last = rainKeysList.last().split("/").last().split(".png").first()


            val dataResponse = DataResponse(
                    radarId = event.radarId,
                    useCache = true,
                    size = event.size,
                    animationType = event.animationType,
                    targetBucket = targetBucket,
                    targetKey = "${event.radarId}/${event.size}/${first}_${last}.${event.animationType}",
                    sourceBucket = sourceBucket,
                    layerKeys = listLayerKeys(event.radarId),
                    freezeKeys = freezes,
                    rainKeys = rainKeysList!!,
                    first = first,
                    last = last
            )

            val proxyResponse = ProxyResponse(body = "", statusCode = 200, headers = mapOf(
                    Pair("Content-Type", "application/json"),
                    Pair("Access-Control-Allow-Origin", "*")))

            if (encryption == "ON") {
                timed.start(ENCRYPT)
                val dataResponseEncryption = DataResponseEncryption(animationRequest = Helpers.encrypt(
                        kms = kms,
                        keyId = kmsKeyId,
                        plaintext = mapper.writeValueAsString(dataResponse)
                ), radarId = event.radarId, first = dataResponse.first, last = dataResponse.last)
                timed.end(ENCRYPT)
                println("encryption time: ${timed.elapsed(ENCRYPT).toString()}")

                proxyResponse.body = mapper.writeValueAsString(dataResponseEncryption)
            } else {
                proxyResponse.body = mapper.writeValueAsString(dataResponse)
            }

            mapper.writeValue(output, proxyResponse)
        }
    }
}
