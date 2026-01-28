package io.github.d0ublew.bapp.starter.dataclass

import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.http.message.responses.HttpResponse

data class ChecklistResult(
    val resultId: String,
    val checklist: Checklist,
    val httpRequestResponse: HttpRequestResponse,
    val status: String,
)



