package com.nexus.messenger.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ChatFolder(
    val id: String,
    val name: String,
    val chatIds: List<String>
)

/** Локальные папки чатов (хранятся на устройстве) */
object FoldersStore {
    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("nexus_folders", Context.MODE_PRIVATE)

    fun all(ctx: Context): List<ChatFolder> {
        val raw = prefs(ctx).getString("folders", null) ?: return emptyList()
        val arr = try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
        val out = mutableListOf<ChatFolder>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val ids = mutableListOf<String>()
            val idsArr = o.optJSONArray("chatIds")
            if (idsArr != null) {
                for (k in 0 until idsArr.length()) ids.add(idsArr.optString(k))
            }
            out.add(ChatFolder(o.optString("id"), o.optString("name"), ids))
        }
        return out
    }

    fun save(ctx: Context, folders: List<ChatFolder>) {
        val arr = JSONArray()
        folders.forEach { f ->
            arr.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("chatIds", JSONArray(f.chatIds))
            })
        }
        prefs(ctx).edit().putString("folders", arr.toString()).apply()
    }

    fun upsert(ctx: Context, folder: ChatFolder) {
        val list = all(ctx).toMutableList()
        val idx = list.indexOfFirst { it.id == folder.id }
        if (idx >= 0) list[idx] = folder else list.add(folder)
        save(ctx, list)
    }

    fun delete(ctx: Context, id: String) {
        save(ctx, all(ctx).filter { it.id != id })
    }
}