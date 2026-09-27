package com.nexus.messenger

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.ChatFolder
import com.nexus.messenger.data.FoldersStore
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp

class FoldersActivity : Activity() {

    private lateinit var listMode: LinearLayout
    private lateinit var editMode: LinearLayout
    private lateinit var foldersCard: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var chatsCard: LinearLayout
    private lateinit var deleteBtn: TextView
    private var editingId: String? = null
    private val chatSwitches = mutableListOf<Pair<String, Switch>>()
    private val allChats = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }

        listMode = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listMode.addView(header("Папки с чатами") { finish() })

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        content.addView(TextView(this).apply {
            text = "📁"
            textSize = 56f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(24) })
        content.addView(Ui.text(this, "Сортируйте чаты по папкам", 15f, R.color.textSecondary),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                topMargin = dp(10)
                bottomMargin = dp(16)
            })

        foldersCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@FoldersActivity)
        }
        content.addView(foldersCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val createRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        createRow.addView(Ui.tileIcon(this, R.drawable.ic_person_add, 0xFF4D7EC2.toInt()),
            LinearLayout.LayoutParams(dp(40), dp(40)))
        createRow.addView(Ui.text(this, "Создать новую папку", 16f, R.color.accentText),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
        createRow.setOnClickListener { openEdit(null) }
        content.addView(createRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })

        scroll.addView(content)
        listMode.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        root.addView(listMode, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        editMode = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        editMode.addView(header("Папка") { showList() })

        val editScroll = ScrollView(this)
        val editContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        val nameCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@FoldersActivity)
        }
        nameCard.addView(Ui.text(this, "Название папки", 13f, R.color.accentText, true)
            .apply { setPadding(dp(16), dp(14), dp(16), dp(6)) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        nameInput = EditText(this).apply {
            hint = "Например: Работа"
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = null
            setPadding(dp(16), dp(8), dp(16), dp(16))
            textSize = 16f
            maxLines = 1
        }
        nameCard.addView(nameInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        editContent.addView(nameCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        editContent.addView(Ui.text(this, "Чаты в папке", 13f, R.color.accentText, true)
            .apply { setPadding(dp(4), dp(16), dp(4), dp(6)) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        chatsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@FoldersActivity)
        }
        editContent.addView(chatsCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val saveBtn = TextView(this).apply {
            text = "Сохранить папку"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            background = Ui.pill(this@FoldersActivity, R.color.accent)
            elevation = dp(6).toFloat()
        }
        saveBtn.setOnClickListener { saveFolder() }
        editContent.addView(saveBtn, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)).apply { topMargin = dp(18) })

        deleteBtn = TextView(this).apply {
            text = "Удалить папку"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.danger))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            visibility = View.GONE
        }
        deleteBtn.setOnClickListener {
            editingId?.let { id ->
                NxDialog(this)
                    .message("Удалить папку? Чаты останутся в общем списке.")
                    .button("Удалить") {
                        FoldersStore.delete(this, id)
                        Ui.snackbar(this, "Папка удалена")
                        showList()
                    }
                    .button("Отмена") {}
                    .show()
            }
        }
        editContent.addView(deleteBtn, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(6) })

        editScroll.addView(editContent)
        editMode.addView(editScroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        root.addView(editMode, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        setContentView(root)
        showList()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun header(title: String, onBack: () -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(color(R.color.bgSecondary))
        setPadding(dp(4), dp(10), dp(16), dp(10))
        val back = TextView(this@FoldersActivity).apply {
            text = "←"
            textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { onBack() }
        addView(back)
        addView(Ui.text(this@FoldersActivity, title, 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
    }

    private fun showList() {
        editMode.visibility = View.GONE
        listMode.visibility = View.VISIBLE
        foldersCard.removeAllViews()

        val allRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        allRow.addView(Ui.tileIcon(this, R.drawable.ic_chat, 0xFF3FC1C9.toInt()),
            LinearLayout.LayoutParams(dp(40), dp(40)))
        val allMid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        allMid.addView(Ui.text(this, "Все чаты", 16f, R.color.textPrimary),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        allMid.addView(Ui.text(this, "Стандартная папка", 13f, R.color.textSecondary),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
        allRow.addView(allMid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
        foldersCard.addView(allRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val folders = FoldersStore.all(this)
        folders.forEach { f ->
            foldersCard.addView(View(this).apply { setBackgroundColor(color(R.color.divider)) },
                LinearLayout.LayoutParams(MATCH_PARENT, 1).apply { leftMargin = dp(72) })
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }
            row.addView(Ui.tileIcon(this, R.drawable.ic_folder, 0xFF4D7EC2.toInt()),
                LinearLayout.LayoutParams(dp(40), dp(40)))
            val mid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            mid.addView(Ui.text(this, f.name, 16f, R.color.textPrimary),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            mid.addView(Ui.text(this, "Чатов: ${f.chatIds.size}", 13f, R.color.textSecondary),
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
            row.addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
            row.addView(Ui.icon(this, R.drawable.ic_pencil, color(R.color.textMuted)),
                LinearLayout.LayoutParams(dp(20), dp(20)))
            row.setOnClickListener { openEdit(f) }
            foldersCard.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
    }

    private fun openEdit(folder: ChatFolder?) {
        editingId = folder?.id
        nameInput.setText(folder?.name ?: "")
        listMode.visibility = View.GONE
        editMode.visibility = View.VISIBLE
        deleteBtn.visibility = if (folder != null) View.VISIBLE else View.GONE
        loadChats(folder?.chatIds ?: emptyList())
    }

    private fun loadChats(preselected: List<String>) {
        chatSwitches.clear()
        chatsCard.removeAllViews()
        chatsCard.addView(Ui.text(this, "Загрузка чатов…", 14f, R.color.textMuted)
            .apply { setPadding(dp(16), dp(14), dp(16), dp(14)) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        Api.get("/chats") { code, body ->
            runOnUiThread {
                allChats.clear()
                if (code == 200) {
                    val arr = Api.parseArray(body)
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        allChats.add(o.optString("id") to (o.optString("title").takeIf { it.isNotEmpty() && it != "null" } ?: "Чат"))
                    }
                }
                chatsCard.removeAllViews()
                if (allChats.isEmpty()) {
                    chatsCard.addView(Ui.text(this, "Нет доступных чатов", 14f, R.color.textMuted)
                        .apply { setPadding(dp(16), dp(14), dp(16), dp(14)) },
                        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    return@runOnUiThread
                }
                allChats.forEachIndexed { i, (id, title) ->
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(16), dp(12), dp(16), dp(12))
                    }
                    row.addView(Ui.text(this, title, 15f, R.color.textPrimary),
                        LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                    val sw = Ui.switch(this, preselected.contains(id)) { }
                    row.addView(sw, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
                    row.setOnClickListener { sw.isChecked = !sw.isChecked }
                    chatSwitches.add(id to sw)
                    chatsCard.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    if (i < allChats.size - 1) {
                        chatsCard.addView(View(this).apply { setBackgroundColor(color(R.color.divider)) },
                            LinearLayout.LayoutParams(MATCH_PARENT, 1).apply { leftMargin = dp(16) })
                    }
                }
            }
        }
    }

    private fun saveFolder() {
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            Ui.snackbar(this, "Укажите название папки")
            return
        }
        val ids = chatSwitches.filter { it.second.isChecked }.map { it.first }
        FoldersStore.upsert(this, ChatFolder(
            id = editingId ?: "f${System.currentTimeMillis()}",
            name = name,
            chatIds = ids
        ))
        Ui.snackbar(this, "Папка «$name» сохранена")
        showList()
    }
}