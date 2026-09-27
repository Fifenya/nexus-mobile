package com.nexus.messenger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Store
import com.nexus.messenger.data.User
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class CreateGroupActivity : Activity() {
    private lateinit var contentStep1: LinearLayout
    private lateinit var contentStep2: LinearLayout
    private lateinit var search: EditText
    private lateinit var listView: ListView
    private lateinit var nameInput: EditText
    private lateinit var groupAvatar: TextView
    private val results = mutableListOf<User>()
    private val selected = mutableListOf<User>()
    private lateinit var adapter: ArrayAdapter<User>

    private val avatarColors = intArrayOf(
        0xFFE17076.toInt(), 0xFF7BC862.toInt(), 0xFF65AADD.toInt(),
        0xFFA695E7.toInt(), 0xFFEE7AAE.toInt(), 0xFF6EC9CB.toInt(), 0xFFFAA774.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }

        // ── Шаг 1: кого пригласить ──
        contentStep1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(color(R.color.bgSecondary))
            setPadding(dp(4), dp(8), dp(16), dp(8))
        }
        val back = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            imageTintList = ColorStateList.valueOf(color(R.color.accentText))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        back.setOnClickListener { finish() }
        header.addView(back, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(Ui.text(this, "Создать группу", 19f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        titles.addView(Ui.text(this, "до 200 000 участников", 12f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        header.addView(titles, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
        contentStep1.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        search = EditText(this).apply {
            hint = "Кого бы Вы хотели пригласить?"
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = Ui.pill(this@CreateGroupActivity, R.color.bgInput)
            val d = resources.getDrawable(R.drawable.ic_search, null)
            d.setTint(color(R.color.textMuted))
            setCompoundDrawablesWithIntrinsicBounds(d, null, null, null)
            compoundDrawablePadding = dp(12)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            textSize = 15f
            maxLines = 1
        }
        contentStep1.addView(search, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            leftMargin = dp(12); rightMargin = dp(12); topMargin = dp(8); bottomMargin = dp(6)
        })

        adapter = object : ArrayAdapter<User>(this@CreateGroupActivity, 0, results) {
            override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                val u = getItem(pos)!!
                val isSel = selected.any { it.id == u.id }
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(10), dp(16), dp(10))
                }
                row.addView(TextView(context).apply {
                    text = u.username.take(1).uppercase()
                    textSize = 18f
                    gravity = Gravity.CENTER
                    setTextColor(0xFFFFFFFF.toInt())
                    paint.isFakeBoldText = true
                    background = Ui.tileCircle(context, avatarColors[Math.abs(u.id.hashCode()) % avatarColors.size])
                }, LinearLayout.LayoutParams(dp(48), dp(48)))
                val mid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                mid.addView(Ui.text(context, u.displayName ?: u.username, 16f, R.color.textPrimary),
                    LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                row.addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(14) })
                row.addView(TextView(context).apply {
                    text = if (isSel) "✓" else ""
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setTextColor(0xFFFFFFFF.toInt())
                    paint.isFakeBoldText = true
                    background = if (isSel) Ui.tileCircle(context, color(R.color.accent))
                    else GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0x00000000)
                        setStroke(dp(2), color(R.color.textMuted))
                    }
                }, LinearLayout.LayoutParams(dp(26), dp(26)))
                row.setOnClickListener {
                    if (isSel) selected.removeAll { it.id == u.id } else selected.add(u)
                    notifyDataSetChanged()
                }
                return row
            }
        }
        listView = ListView(this).apply {
            this.adapter = this@CreateGroupActivity.adapter
            setBackgroundColor(color(R.color.bgPrimary))
            divider = null
            dividerHeight = 0
        }
        contentStep1.addView(listView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        contentStep1.addView(TextView(this).apply {
            text = "🐤"
            textSize = 56f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        contentStep1.addView(Ui.text(this, "Контактов пока нет", 18f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER })
        contentStep1.addView(Ui.text(this, "Начните вводить имя пользователя выше", 13f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                topMargin = dp(6)
                bottomMargin = dp(80)
            })

        root.addView(contentStep1, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // ── Шаг 2: название ──
        contentStep2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }
        val nameCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.card(this@CreateGroupActivity)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        groupAvatar = TextView(this).apply {
            text = "N"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            background = Ui.tileCircle(this@CreateGroupActivity, 0xFF65AADD.toInt())
        }
        nameCard.addView(groupAvatar, LinearLayout.LayoutParams(dp(64), dp(64)))
        nameInput = EditText(this).apply {
            hint = "Название группы"
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = null
            setPadding(dp(16), dp(8), dp(16), dp(8))
            textSize = 17f
            maxLines = 1
        }
        nameCard.addView(nameInput, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(14) })
        contentStep2.addView(nameCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val autoCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.card(this@CreateGroupActivity)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        autoCard.addView(Ui.icon(this, R.drawable.ic_data, color(R.color.textSecondary)),
            LinearLayout.LayoutParams(dp(28), dp(28)))
        autoCard.addView(Ui.text(this, "Автоудаление сообщений", 16f, R.color.textPrimary),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(14) })
        autoCard.addView(Ui.text(this, "Выкл.", 14f, R.color.accentText))
        autoCard.setOnClickListener { Ui.snackbar(this, "Появится в следующем обновлении") }
        contentStep2.addView(autoCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })

        contentStep2.addView(Ui.text(this, "Участников будет добавлено: ${selected.size}", 13f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(14) })

        root.addView(contentStep2, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // ── FAB ──
        val frame = FrameLayout(this)
        frame.addView(root, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        val fab = ImageView(this).apply {
            setImageResource(R.drawable.ic_check)
            imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
            background = Ui.pill(this@CreateGroupActivity, R.color.accent)
            elevation = dp(6).toFloat()
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        fab.setOnClickListener { onFab() }
        frame.addView(fab, FrameLayout.LayoutParams(dp(56), dp(56)).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            rightMargin = dp(20); bottomMargin = dp(28)
        })
        setContentView(frame)

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s?.toString()?.trim() ?: ""
                if (q.length >= 2) {
                    Api.get("/users/search?q=" + URLEncoder.encode(q, "UTF-8")) { code, body ->
                        runOnUiThread {
                            results.clear()
                            if (code == 200) {
                                val arr = Api.parseArray(body)
                                for (i in 0 until arr.length()) {
                                    val u = User.fromJson(arr.getJSONObject(i))
                                    if (u.id != Store.user?.id) results.add(u)
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }
                    }
                } else {
                    results.clear()
                    adapter.notifyDataSetChanged()
                }
            }
        })

        nameInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                groupAvatar.text = (s?.toString()?.take(1)?.uppercase() ?: "N").ifEmpty { "N" }
            }
        })
    }

    private var step = 1

    private fun onFab() {
        if (step == 1) {
            if (selected.isEmpty()) {
                Ui.snackbar(this, "Выберите хотя бы одного участника")
                return
            }
            step = 2
            contentStep1.visibility = View.GONE
            contentStep2.visibility = View.VISIBLE
        } else {
            create()
        }
    }

    private fun create() {
        val title = nameInput.text.toString().trim()
        if (title.isEmpty()) {
            Ui.snackbar(this, "Укажите название группы")
            return
        }
        val ids = JSONArray()
        selected.forEach { ids.put(it.id) }
        Api.post("/chats", JSONObject()
            .put("title", title)
            .put("type", "GROUP")
            .put("userIds", ids)
        ) { code, body ->
            runOnUiThread {
                if (code in 200..299) {
                    val j = Api.parseObj(body)
                    val chatId = j?.optString("id") ?: ""
                    Ui.snackbar(this, "Группа создана ✓")
                    if (chatId.isNotEmpty()) {
                        startActivity(Intent(this, ChatActivity::class.java)
                            .putExtra("chatId", chatId)
                            .putExtra("title", title))
                    }
                    finish()
                } else {
                    Ui.snackbar(this, Api.friendlyError(body))
                }
            }
        }
    }

    private fun color(res: Int): Int = resources.getColor(res, null)
}