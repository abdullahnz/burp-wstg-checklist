package io.github.d0ublew.bapp.starter

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.logging.Logging
import java.util.UUID
import io.github.d0ublew.bapp.starter.dataclass.CHECKLIST

class Storage (
    private val api: MontoyaApi,
    private val logger: Logging
) {
    private val persistence = api.persistence().extensionData()
    private val persistenceSignatureKey = "wstg"

    private val checklists = CHECKLIST.groupBy { it.id }

    fun add(checklistId: String, httpRequestResponse: HttpRequestResponse, status: String) {
        val resultId = UUID.randomUUID().toString()

        val key = "$persistenceSignatureKey:$checklistId:$resultId:$status"
        persistence.setHttpRequestResponse(key, httpRequestResponse)
    }

    fun debug() {
        logger.logToOutput("========== WSTG STORAGE DEBUG ==========")

        persistence.httpRequestResponseKeys()
            .asSequence()
            .filter { it.startsWith(persistenceSignatureKey) }
            .forEach { key ->

                logger.logToOutput("Key: $key")

                val parts = key.split(":")

                if (parts.size < 4) {
                    logger.logToOutput("Invalid key format, skipped")
                    return@forEach
                }

                val checklistId = parts[1]
                val resultId = parts[2]
                val status = parts[3]

                logger.logToOutput("Checklist : $checklistId")
                logger.logToOutput("Result ID : $resultId")
                logger.logToOutput("Status    : $status")

                val checklist = checklists[checklistId]
                logger.logToOutput("Checklist Meta: $checklist")

                val rr = persistence.getHttpRequestResponse(key)
                if (rr == null) {
                    logger.logToOutput("⚠ No HttpRequestResponse found")
                    return@forEach
                }

                val request = rr.request()
                val response = rr.response()

                logger.logToOutput("--- HTTP REQUEST ---")
                logger.logToOutput(request.toString())

                logger.logToOutput("--- HTTP RESPONSE ---")
                logger.logToOutput(response.toString())
            }

        logger.logToOutput("=======================================")
    }


}