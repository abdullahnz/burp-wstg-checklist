package io.github.d0ublew.bapp.starter

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.logging.Logging
import dataclass.ChecklistResultMetadata
import io.github.d0ublew.bapp.starter.dataclass.CHECKLIST
import io.github.d0ublew.bapp.starter.dataclass.ChecklistResult
import java.util.*

class Storage(
    private val api: MontoyaApi,
    private val logger: Logging
) {
    private val persistence = api.persistence().extensionData()
    private val gson = com.google.gson.Gson()
    private val persistenceSignatureKey = PERSISTENCE_SIGNATURE_KEY

    private val checklists = CHECKLIST.groupBy { it.id }.mapValues { it.value.first() }

    fun add(
        checklistId: String,
        httpRequestResponse: HttpRequestResponse,
        status: String
    ) {
        val resultId = UUID.randomUUID().toString()

        val meta = ChecklistResultMetadata(
            resultId = resultId,
            checklistId = checklistId,
            status = status
        )

        persistence.setHttpRequestResponse(
            httpKey(resultId),
            httpRequestResponse
        )

        persistence.setString(
            metaKey(resultId),
            gson.toJson(meta)
        )
    }

    fun updateStatus(resultId: String, newStatus: String) {
        val meta = loadMeta(resultId) ?: return
        meta.status = newStatus

        persistence.setString(
            metaKey(resultId),
            gson.toJson(meta)
        )
    }

    fun delete(resultId: String) {
        persistence.deleteHttpRequestResponse(httpKey(resultId))
        persistence.deleteString(metaKey(resultId))
    }

    fun get(): ArrayList<ChecklistResult> {
        val results = arrayListOf<ChecklistResult>()

        persistence.stringKeys()
            .asSequence()
            .filter { it.startsWith("$persistenceSignatureKey:meta:") }
            .forEach { key ->
                val resultId = key.substringAfterLast(":")
                val meta = loadMeta(resultId) ?: return@forEach
                val http = persistence.getHttpRequestResponse(httpKey(resultId)) ?: return@forEach

                val checklist = checklists[meta.checklistId] ?: return@forEach

                results.add(
                    ChecklistResult(
                        resultId = resultId,
                        checklist = checklist,
                        httpRequestResponse = http,
                        status = meta.status
                    )
                )
            }

        return results
    }

    fun getByResultId(resultId: String): ChecklistResult? {
        val meta = loadMeta(resultId) ?: return null
        val http = persistence.getHttpRequestResponse(httpKey(resultId)) ?: return null
        val checklist = checklists[meta.checklistId] ?: return null

        return ChecklistResult(
            resultId = resultId,
            checklist = checklist,
            httpRequestResponse = http,
            status = meta.status
        )
    }


    private fun loadMeta(resultId: String): ChecklistResultMetadata? {
        val json = persistence.getString(metaKey(resultId)) ?: return null
        return gson.fromJson(json, ChecklistResultMetadata::class.java)
    }

    private fun httpKey(id: String) =
        "$persistenceSignatureKey:http:$id"

    private fun metaKey(id: String) =
        "$persistenceSignatureKey:meta:$id"
}