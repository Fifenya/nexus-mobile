package com.nexus.messenger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
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
import com.nexus.messenger.ui.BottomNav
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONArray
import java.net.URLEncoder

class ContactsActivity : Activity() {
    private lateinit var search: EditText
    private lateinit var listView: ListView
    private lateinit var emptyView: LinearLayout
    private val results = mutableListOf<User>()
    private lateinit var adapter: ArrayAdapter<User>

    private val avatarColors = intArrayOf(
        0xFFE17076.toInt(), 0xFF7BC862.toInt(), 0xFF65AADD.toInt(),
        0xFFA695E7.toInt(), 0xFFEE7AAE.toInt(), 0xFF6EC9CB.toInt(), 0xFFFAA774.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this)
        frame.setBackgroundColor(color(R.color.bgPrimary))

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        root.addView(Ui.text(this, "Контакты", 22f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setPadding(dp(16), dp(14), dp(16), dp(10))
            })

        search = EditText(this).apply {
            hint = "Поиск по имени пользователя"
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = Ui.pill(this@ContactsActivity, R.color.bgInput)
            val d = resources.getDrawable(R.drawable.ic_search, null)
            d.setTint(color(R.color.textMuted))
            setCompoundDrawablesWithIntrinsicBounds(d, null, null, null)
            compoundDrawablePadding = dp(12)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            textSize = 15f
            maxLines = 1
        }
        root.addView(search, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            leftMargin = dp(12); rightMargin = dp(12); bottomMargin = dp(6)
        })

        adapter = object : ArrayAdapter<User>(this@ContactsActivity, 0, results) {
            override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                val u = getItem(pos)!!
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(10), dp(16), dp(10))
                }
                row.addView(TextView(context).apply {
                    text = u.username.take(1).uppercase()
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setTextColor(0xFFFFFFFF.toInt())
                    paint.isFakeBoldText = true
                    background = Ui.tileCircle(context, avatarColors[Math.abs(u.id.hashCode()) % avatarColors.size])
                }, LinearLayout.LayoutParams(dp(54), dp(54)))
                val mid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                mid.addView(Ui.text(context, u.displayName ?: u.username, 16f, R.color.textPrimary, true),
                    LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                mid.addView(Ui.text(context, "@${u.username}", 13f, R.color.textSecondary),
                    LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
                row.addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(14) })
                row.setOnClickListener { openPrivate(u) }
                return row
            }
        }

        listView = ListView(this).apply {
            this.adapter = this@ContactsActivity.adapter
            setBackgroundColor(color(R.color.bgPrimary))
            divider = null
            dividerHeight = 0
            clipToPadding = false
            setPadding(0, dp(4), 0, dp(110))
            visibility = View.GONE
        }
        root.addView(listView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        emptyView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(32), dp(32), dp(120))
        }
        emptyView.addView(TextView(this).apply {
            text = "🐤"
            textSize = 64f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        emptyView.addView(Ui.text(this, "Добавить контакты", 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(16) })
        emptyView.addView(Ui.text(this, "Найдите друзей по имени пользователя — Nexus покажет всех, кто зарегистрирован на сервере.", 14f, R.color.textSecondary),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) })
        val newBtn = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = Ui.pill(this@ContactsActivity, R.color.accent)
            elevation = dp(6).toFloat()
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        newBtn.addView(Ui.icon(this, R.drawable.ic_person_add, 0xFFFFFFFF.toInt()),
            LinearLayout.LayoutParams(dp(20), dp(20)))
        newBtn.addView(Ui.text(this, "Новый контакт", 15f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = dp(8) })
        newBtn.setOnClickListener { search.requestFocus() }
        emptyView.addView(newBtn, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(24) })
        root.addView(emptyView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        frame.addView(root, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        BottomNav.attach(frame, this, "contacts")
        setContentView(frame)

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s?.toString()?.trim() ?: ""
                if (q.length >= 2) doSearch(q) else showEmpty()
            }
        })
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun showEmpty() {
        results.clear()
        adapter.notifyDataSetChanged()
        listView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
    }

    private fun doSearch(q: String) {
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
                listView.visibility = if (results.isEmpty()) View.GONE else View.VISIBLE
                emptyView.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                if (results.isEmpty()) {
                    emptyView.getChildAt(1)?.let { (it as TextView).text = "Никого не нашли" }
                } else {
                    emptyView.visibility = View.GONE
                }
            }
        }
    }

    private fun openPrivate(u: User) {
        Api.post("/chats", org.json.JSONObject()
            .put("userIds", JSONArray().put(u.id))
            .put("type", "PRIVATE")
        ) { code, body ->
            runOnUiThread {
                if (code in 200..299) {
                    val j = Api.parseObj(body)
                    val chatId = j?.optString("id") ?: ""
                    if (chatId.isNotEmpty()) {
                        startActivity(Intent(this, ChatActivity::class.java)
                            .putExtra("chatId", chatId)
                            .putExtra("title", u.displayName ?: u.username))
                    } else {
                        com.nexus.messenger.ui.Ui.snackbar(this, "Чат создан, обновите список")
                    }
                } else {
                    Ui.snackbar(this, Api.friendlyError(body))
                }
            }
        }
    }
}