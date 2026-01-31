package io.github.d0ublew.bapp.starter

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.logging.Logging
import io.github.d0ublew.bapp.starter.dataclass.CHECKLIST
import io.github.d0ublew.bapp.starter.dataclass.ChecklistResult
import java.util.*


class Storage (
    private val api: MontoyaApi,
    private val logger: Logging
) {
    private val persistence = api.persistence().extensionData()
    private val persistenceSignatureKey = PERSISTENCE_SIGNATURE_KEY

    private val checklists = CHECKLIST.groupBy { it.id }.mapValues { it.value.first() }

    fun add(checklistId: String, httpRequestResponse: HttpRequestResponse, status: String) {
        val resultId = UUID.randomUUID().toString()
        val key = generateKey(checklistId, resultId, status)

        persistence.setHttpRequestResponse(key, httpRequestResponse)
    }

    fun delete(checklistId: String, resultId: String, status: String) {
        val key = generateKey(checklistId, resultId, status)

        persistence.deleteHttpRequestResponse(key)
    }

    fun get(): ArrayList<ChecklistResult> {
        val results: ArrayList<ChecklistResult> = arrayListOf()


        persistence.httpRequestResponseKeys()
            .asSequence()
            .filter { it.startsWith(persistenceSignatureKey) }
            .forEach { key ->
                val parts = key.split(":")

                if (parts.size != 4) {
                    return@forEach
                }

                val checklistId = parts[1]
                val resultId = parts[2]
                val status = parts[3]

                val checklist = checklists[checklistId] ?: return@forEach
                val httpRequestResponse = persistence.getHttpRequestResponse(key) ?: return@forEach


                val result = ChecklistResult(
                    resultId,
                    checklist,
                    httpRequestResponse,
                    status
                )

                results.add(result)

            }

        return results
    }

    private fun generateKey(checklistId: String, resultId: String, status: String): String{
        return "$persistenceSignatureKey:$checklistId:$resultId:$status"
    }
}