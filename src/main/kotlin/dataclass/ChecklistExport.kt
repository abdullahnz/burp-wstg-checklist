package io.github.d0ublew.bapp.starter.dataclass

data class ChecklistExport(
    val id: String,
    val title: String,
    val name: String,
    val request: ChecklistExportRequest,
    val response: ChecklistExportResponse?,
    val status: String,
)

data class ChecklistExportRequest(
    val method: String,
    val url: String,
    val headers: List<String>,
    val body: String,
    val raw: String
)

data class ChecklistExportResponse(
    val statusCode: Short,
    val headers: List<String>,
    val body: String,
    val raw: String
)
