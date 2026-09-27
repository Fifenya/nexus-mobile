package com.nexus.messenger.data

import org.json.JSONObject

/**
 * ВРЕМЕННО ОТКЛЮЧЕНО: тестовая симуляция сервера (режим testdevapp).
 * Заглушка оставлена, чтобы Api.kt и остальные файлы продолжали
 * компилироваться без правок. Полная версия с эхо-чатом, мотами,
 * группами и ссылками лежала в истории — вернём её позже отдельным
 * коммитом, когда основная сборка стабилизируется.
 */
object MockServer {
    fun handle(method: String, path: String, body: JSONObject?, onResult: (Int, String) -> Unit) {
        onResult(503, "{\"statusCode\":503,\"message\":\"Test mode temporarily disabled\"}")
    }
}