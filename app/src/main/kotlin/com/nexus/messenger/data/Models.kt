package com.nexus.messenger.data

import org.json.JSONObject

/** Безопасное чтение строки: отсутствует / null / "null" / "" → null */
private fun str(j: JSONObject, key: String): String? {
    if (!j.has(key) || j.isNull(key)) return null
    val v = j.optString(key, "")
    return if (v.isEmpty() || v == "null") null else v
}

data class User(
    val id: String,
    val username: String,
    val displayName: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val online: Boolean = false,
    val bio: String? = null
) {
    companion object {
        fun fromJson(j: JSONObject) = User(
            str(j, "id") ?: "",
            str(j, "username") ?: "",
            str(j, "displayName"),
            str(j, "email"),
            str(j, "avatar") ?: str(j, "avatarUrl"),
            j.optBoolean("online"),
            str(j, "bio")
        )
    }
}

data class Chat(
    val id: String,
    val title: String?,
    val type: String? = null,
    val pinned: Boolean = false,
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0
) {
    companion object {
        fun fromJson(j: JSONObject): Chat {
            val lm = j.optJSONObject("lastMessage")
            return Chat(
                str(j, "id") ?: "",
                str(j, "title"),
                str(j, "type"),
                j.optBoolean("pinned"),
                lm?.let { str(it, "text") },
                lm?.let { str(it, "createdAt") },
                j.optInt("unreadCount")
            )
        }
    }
}

data class Message(
    val id: String,
    val text: String,
    val createdAt: String,
    val updatedAt: String? = null,
    val authorId: String,
    val authorName: String,
    val replyToId: String? = null,
    val replyText: String? = null,
    val replyAuthor: String? = null
) {
    companion object {
        fun fromJson(j: JSONObject): Message {
            val author = j.optJSONObject("author") ?: JSONObject()
            val reply = j.optJSONObject("replyTo")
            return Message(
                str(j, "id") ?: "",
                str(j, "text") ?: "",
                str(j, "createdAt") ?: "",
                str(j, "updatedAt"),
                str(author, "id") ?: "",
                str(author, "username") ?: "",
                reply?.let { str(it, "id") },
                reply?.let { str(it, "text") },
                reply?.let { str(it, "author") }
            )
        }
    }
}