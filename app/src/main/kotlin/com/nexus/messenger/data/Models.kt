package com.nexus.messenger.data

import org.json.JSONObject

private fun str(j: JSONObject, key: String): String? {
    if (!j.has(key) || j.isNull(key)) return null
    val v = j.optString(key, "")
    return if (v.isEmpty() || v == "null") null else v
}

data class Reaction(
    val emoji: String,
    val userId: String
)

data class Attachment(
    val type: String,
    val url: String,
    val duration: Int? = null,
    val size: Int? = null,
    val mimeType: String? = null
)

data class Mote(
    val id: String,
    val name: String,
    val url: String,
    val tags: String,
    val ownerId: String
) {
    companion object {
        fun fromJson(j: JSONObject) = Mote(
            str(j, "id") ?: "",
            str(j, "name") ?: "Мот",
            str(j, "url") ?: "",
            str(j, "tags") ?: "",
            str(j, "ownerId") ?: ""
        )
    }
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
    val viewIds: List<String> = emptyList(),
    val reactions: List<Reaction> = emptyList(),
    val attachments: List<Attachment> = emptyList()
) {
    companion object {
        fun fromJson(j: JSONObject): Message {
            val senderObj = j.optJSONObject("sender")
                ?: j.optJSONObject("author")
                ?: JSONObject()

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

            val reactionsArr = j.optJSONArray("reactions")
            val reactions = mutableListOf<Reaction>()
            if (reactionsArr != null) {
                for (i in 0 until reactionsArr.length()) {
                    val r = reactionsArr.optJSONObject(i) ?: continue
                    val emoji = r.optString("emoji")
                    val uid = r.optJSONObject("user")?.optString("id")
                        ?: r.optString("userId")
                        ?: ""
                    if (emoji.isNotEmpty() && emoji != "null") {
                        reactions.add(Reaction(emoji, uid))
                    }
                }
            }

            val attArr = j.optJSONArray("attachments")
            val attachments = mutableListOf<Attachment>()
            if (attArr != null) {
                for (i in 0 until attArr.length()) {
                    val a = attArr.optJSONObject(i) ?: continue
                    val url = str(a, "url") ?: continue
                    attachments.add(Attachment(
                        type = str(a, "type") ?: "image",
                        url = url,
                        duration = if (a.isNull("duration")) null else a.optInt("duration"),
                        size = if (a.isNull("size")) null else a.optInt("size"),
                        mimeType = str(a, "mimeType")
                    ))
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
                views,
                reactions,
                attachments
            )
        }
    }
}