package com.nexus.messenger.data

import org.json.JSONObject

object MockServer {
    fun handle(
        @Suppress("UNUSED_PARAMETER") method: String,
        @Suppress("UNUSED_PARAMETER") path: String,
        @Suppress("UNUSED_PARAMETER") body: JSONObject?,
        onResult: (Int, String) -> Unit
    ) {
        onResult(503, "{\"statusCode\":503,\"message\":\"Test mode temporarily disabled\"}")
    }
}