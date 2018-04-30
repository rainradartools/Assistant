package org.rainradartools.assistant

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.rainradartools.assistant.core.Helpers
import org.rainradartools.assistant.domain.DataResponseEncryption
import org.rainradartools.assistant.domain.ProxyRequest
import org.rainradartools.assistant.domain.ProxyResponse
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Paths


fun main(args: Array<String>) {
    println("main function")

    val mapper = jacksonObjectMapper()
    val gson = GsonBuilder().setPrettyPrinting().create()
    val jp = JsonParser()

    var m = Handler(Helpers.loadConfigFromFile(Paths.get("conf/localEnvironment.yml")))
    val body = File("requestJSON/request1.json").readText()

    val proxyRequest = ProxyRequest(body = body, httpMethod = "POST")

    val outStream = ByteArrayOutputStream()

    var startTime = System.nanoTime()
    m.handler(mapper.writeValueAsString(proxyRequest).byteInputStream(), outStream)
    var endTime = System.nanoTime()

    println(gson.toJson(jp.parse(outStream.toString())))
    println("timed: ${(endTime - startTime) / 1000000 }")

    val kms = AWSKMSClientBuilder.standard()
    .withCredentials(ProfileCredentialsProvider(""))
            .withRegion("")
            .build()

    println(gson.toJson(jp.parse(outStream.toString())))
}
