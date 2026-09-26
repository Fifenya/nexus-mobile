e fun color(res: Int): Int = resources.getColor(res, null)

    private fun showMenu() {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setItems(arrayOf("Обновить", "Адрес сервера")) { _, i ->
                when (i) {
                    0 -> loadChats()
                    1 -> showServerDialog()
                }
            }
            .show()
    }

    private fun showServerDialog() {
        val input = EditText(this).apply {
            setText(com.nexus.messenger.data.Store.apiBase)
            setTextColor(color(R.color.textPrimary))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Адрес сервера")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                com.nexus.messenger.data.Store.apiBase = input.text.toString().trim()
                Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun loadChats() {
        Api.get("/chats") { code, body ->
            runOnUiThread {
                chats.clear()
                if (code == 200) {
                    val arr = Api.parseArray(body)
                    val temp = mutableListOf<Chat>()
                    for (i in 0 until arr.length()) temp.add(Chat.fromJson(arr.getJSONObject(i)))
                    temp.sortWith(compareByDescending<Chat> { it.pinned })
                    chats.addAll(temp)
                    chatAdapter.notifyDataSetChanged()
                }
                emptyText.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
                listView.visibility = if (chats.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun createChat() {
        val input = EditText(this).apply {
            hint = "Название чата"
            setTextColor(color(R.color.textPrimary))
            setHintTextColor(color(R.color.textMuted))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Новый чат")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val title = input.text.toString().trim()
                Api.post("/chats", JSONObject().put("title", title).put("userIds", org.json.JSONArray())) { code, body ->
                    runOnUiThread {
                        if (code == 200 || code == 201) loadChats()
                        else Toast.makeText(this, "Не удалось создать чат", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun formatTime(s: String?): String {
        if (s.isNullOrEmpty()) return ""
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(s) ?: return ""
            val diff = (System.currentTimeMillis() - date.time) / 86400000L
            when {
                diff == 0L -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                diff == 1L -> "Вчера"
                diff < 7 -> SimpleDateFormat("EEE", Locale("ru")).format(date)
                else -> SimpleDateFormat("dd.MM", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) { "" }
    }
}
