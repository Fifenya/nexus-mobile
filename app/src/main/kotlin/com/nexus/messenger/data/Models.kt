package com.nexus.messenger.data

import org.json.JSONObject

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
            j.optString("id"),
            j.optString("username"),
            j.optString("displayName").takeIf { it.isNotEmpty() },
            j.optString("email").takeIf { it.isNotEmpty() },
            j.optString("avatar").takeIf { it.isNotEmpty() },
            j.optBoolean("online"),
            j.optString("bio").takeIf { it.isNotEmpty() }
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
        fun fromJson(j: JSONObject) = Chat(
            j.optString("id"),
            j.optString("title").takeIf { it.isNotEmpty() },
            j.optString("type").takeIf { it.isNotEmpty() },
            j.optBoolean("pinned"),
            j.optJSONObject("lastMessage")?.optString("text"),
            j.optJSONObject("lastMessage")?.optString("createdAt"),
            j.optInt("unreadCount")
        )
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
                j.optString("id"),
                j.optString("text"),
                j.optString("createdAt"),
                j.optString("updatedAt").takeIf { it.isNotEmpty() },
                author.optString("id"),
                author.optString("username"),
                reply?.optString("id")?.takeIf { it.isNotEmpty() },
                reply?.optString("text"),
                reply?.optString("author")
            )
        }
    }
}