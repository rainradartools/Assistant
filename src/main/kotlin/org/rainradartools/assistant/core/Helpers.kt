package org.rainradartools.assistant.core

import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.DecryptRequest
import com.amazonaws.services.kms.model.EncryptRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.rainradartools.assistant.domain.ConfigDtc
import org.apache.commons.codec.binary.Base64
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path


class Helpers {

    companion object {
        fun loadConfigFromFile(path: Path): ConfigDtc {
            val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
            mapper.registerModule(KotlinModule()) // Enable Kotlin support

            return Files.newBufferedReader(path).use {
                mapper.readValue(it, ConfigDtc::class.java)
            }
        }

        fun convertStreamToString(inStream: InputStream): String {
            val s = java.util.Scanner(inStream).useDelimiter("\\A")
            return if (s.hasNext()) s.next() else ""
        }

        fun encrypt(kms: AWSKMS, keyId: String, plaintext: String): List<String> {

            val maxBytes = 4096

            val plaintextBytes = plaintext.toByteArray(StandardCharsets.UTF_8)
            val bytesSize = plaintextBytes.size
            val numSections = Math.ceil((bytesSize.toDouble() / maxBytes.toDouble())).toInt()

            val encStrings = mutableListOf<String>()

            for (i in 0..numSections-1) {
                val startIdx = i * maxBytes
                var endIdx: Int

                if (i == numSections-1) {
                    endIdx = bytesSize - 1
                } else {
                    endIdx = ((i+1) * maxBytes) - 1
                }

                val encReq = EncryptRequest()
                encReq.keyId = keyId
                encReq.plaintext = ByteBuffer.wrap(plaintextBytes.sliceArray(startIdx..endIdx))
                var ciphertext = kms.encrypt(encReq).ciphertextBlob
                val enbase64 = Base64.encodeBase64(ciphertext.array())
                encStrings.add(enbase64.toString(charset = Charset.defaultCharset()))
            }
            return encStrings
        }

        fun decrypt(kms: AWSKMS, keyId: String, encStrings: List<String>): String {

            val strList = mutableListOf<String>()

            encStrings.forEach {
                val debase64 = Base64.decodeBase64(it.toByteArray())
                val ciphertext = ByteBuffer.wrap(debase64)
                val decReq1 = DecryptRequest()
                decReq1.ciphertextBlob = ciphertext
                val decrypted = kms.decrypt(decReq1).plaintext
                val decryptedStr = String(decrypted.array(), StandardCharsets.UTF_8)
                strList.add(decryptedStr)
            }

            return strList.joinToString("")
        }
    }
}