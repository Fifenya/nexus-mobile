package com.nexus.messenger.data

import org.json.JSONObject

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
    val bio: String? = null,
    val onlineStatus: String? = null,
    val lastSeenAt: String? = null
) {
    companion object {
        fun fromJson(j: JSONObject) = User(
            str(j, "id") ?: "",
            str(j, "username") ?: "",
            str(j, "displayName"),
            str(j, "email"),
            str(j, "avatar") ?: str(j, "avatarUrl"),
            j.optBoolean("online"),
            str(j, "bio"),
            str(j, "onlineStatus"),
            str(j, "lastSeenAt")
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
    val replyAuthor: String? = null,
    val viewIds: List<String> = emptyList()
) {
    companion object {
        fun fromJson(j: JSONObject): Message {
            // Бэкенд возвращает "sender", но поддерживаем и "author" для совместимости
            val senderObj = j.optJSONObject("sender")
                ?: j.optJSONObject("author")
                ?: JSONObject()

            // Если sender это просто строка (id) — берём его
            val senderId = if (senderObj.length() == 0) {
                str(j, "senderId") ?: str(j, "authorId") ?: ""
            } else {
                str(senderObj, "id") ?: ""
            }
            val senderName = if (senderObj.length() == 0) {
                str(j, "senderName") ?: str(j, "authorName") ?: ""
            } else {
                str(senderObj, "username") ?: str(senderObj, "displayName") ?: ""
            }

            val reply = j.optJSONObject("replyTo")
            val viewsArr = j.optJSONArray("views")
            val views = mutableListOf<String>()
            if (viewsArr != null) {
                for (i in 0 until viewsArr.length()) {
                    val v = viewsArr.optJSONObject(i) ?: continue
                    val uid = v.optJSONObject("user")?.optString("id")
                        ?: v.optString("userId")
                    if (uid.isNotEmpty() && uid != "null") views.add(uid)
                }
            }
            return Message(
                str(j, "id") ?: "",
                str(j, "text") ?: "",
                str(j, "createdAt") ?: "",
                str(j, "updatedAt"),
                senderId,
                senderName,
                reply?.let { str(it, "id") },
                reply?.let { str(it, "text") },
                reply?.let { str(it, "author") },
                views
            )
        }
    }
}
