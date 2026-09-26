package com.nexus.messenger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Store
import com.nexus.messenger.data.User
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONObject

class AccountActivity : Activity() {
    private lateinit var firstInput: EditText
    private lateinit var lastInput: EditText
    private lateinit var bioInput: EditText
    private lateinit var bioCounter: TextView
    private lateinit var usernameTitle: TextView

    private val BLUE = 0xFF4D7EC2.toInt()
    private val ORANGE = 0xFFE09A3F.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }

        // ── Шапка: назад + Аккаунт + сохранить ──
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(color(R.color.bgSecondary))
            setPadding(dp(4), dp(10), dp(12), dp(10))
        }
        val back = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            imageTintList = ColorStateList.valueOf(color(R.color.textPrimary))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        back.setOnClickListener { finish() }
        header.addView(back, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        header.addView(
            Ui.text(this, "Аккаунт", 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) }
        )
        val save = ImageView(this).apply {
            setImageResource(R.drawable.ic_check)
            imageTintList = ColorStateList.valueOf(color(R.color.textPrimary))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        save.setOnClickListener { saveProfile() }
        header.addView(save, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        // ── Карточка: Ваше имя ──
        val nameCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@AccountActivity)
        }
        nameCard.addView(sectionLabel("Ваше имя"))
        firstInput = EditText(this).apply {
            hint = "Имя"
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = null
            setPadding(dp(16), dp(14), dp(16), dp(14))
            textSize = 16f
            maxLines = 1
        }
        nameCard.addView(firstInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        nameCard.addView(divider(), LinearLayout.LayoutParams(MATCH_PARENT, 1).apply {
            leftMargin = dp(16); rightMargin = dp(16)
        })
        lastInput = EditText(this).apply {
            hint = "Фамилия"
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = null
            setPadding(dp(16), dp(14), dp(16), dp(14))
            textSize = 16f
            maxLines = 1
        }
        nameCard.addView(lastInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        content.addView(nameCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // ── Карточка: О себе + счётчик ──
        val bioCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.card(this@AccountActivity)
        }
        bioInput = EditText(this).apply {
            hint = "«О себе»"
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = null
            setPadding(dp(16), dp(16), dp(8), dp(16))
            textSize = 16f
            maxLines = 1
            filters = arrayOf(InputFilter.LengthFilter(70))
        }
        bioCard.addView(bioInput, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        bioCounter = Ui.text(this, "70", 14f, R.color.textMuted)
        bioCard.addView(bioCounter, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            rightMargin = dp(16)
        })
        content.addView(bioCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(10)
        })
        content.addView(
            Ui.text(this, "Напишите немного о себе.", 13f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = dp(10); leftMargin = dp(4); bottomMargin = dp(6)
            }
        )

        bioInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                bioCounter.text = "${70 - (s?.length ?: 0)}"
            }
        })

        // ── Карточка: Информация о Вас ──
        val infoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@AccountActivity)
        }
        infoCard.addView(sectionLabel("Информация о Вас"))

        usernameTitle = Ui.text(this, "@${Store.user?.username ?: "—"}", 16f, R.color.textPrimary)
        infoCard.addView(
            rowView(tileText("@"), usernameTitle, "Имя пользователя") {
                Ui.snackbar(this, "Имя пользователя изменить нельзя")
            },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        )
        infoCard.addView(dividerInset(), LinearLayout.LayoutParams(MATCH_PARENT, 1))
        infoCard.addView(
            rowView(Ui.tileIcon(this, R.drawable.ic_data, BLUE),
                Ui.text(this, "Моя статистика", 16f, R.color.textPrimary),
                "Сообщения, чаты, реакции") { showStats() },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        )
        content.addView(infoCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(10)
        })

        // ── Карточка: аккаунты и выход ──
        val accCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@AccountActivity)
        }
        accCard.addView(
            rowView(Ui.icon(this, R.drawable.ic_person_add, color(R.color.accentText)),
                Ui.text(this, "Добавить аккаунт", 16f, R.color.accentText), null) {
                Ui.snackbar(this, "Несколько аккаунтов появятся позже")
            },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        )
        accCard.addView(dividerInset(), LinearLayout.LayoutParams(MATCH_PARENT, 1))
        accCard.addView(
            rowView(Ui.icon(this, R.drawable.ic_logout, color(R.color.danger)),
                Ui.text(this, "Выход", 16f, R.color.danger), null) {
                NxDialog(this)
                    .message("Выйти из аккаунта?")
                    .button("Выйти") {
                        Store.logout()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finishAffinity()
                    }
                    .button("Отмена") {}
                    .show()
            },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        )
        content.addView(accCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(10)
        })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)

        load()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun divider() = View(this).apply { setBackgroundColor(color(R.color.divider)) }

    private fun dividerInset() = View(this).apply {
        setBackgroundColor(color(R.color.divider))
    }.also { it.setPadding(0, 0, 0, 0) }

    private fun sectionLabel(s: String): TextView =
        Ui.text(this, s, 13f, R.color.accentText, true).apply {
            setPadding(dp(16), dp(14), dp(16), dp(6))
        }

    /** Плитка с текстом вместо иконки (для @) */
    private fun tileText(symbol: String): TextView = TextView(this).apply {
        text = symbol
        textSize = 18f
        gravity = Gravity.CENTER
        setTextColor(0xFFFFFFFF.toInt())
        paint.isFakeBoldText = true
        background = Ui.tile(this@AccountActivity, ORANGE)
    }

    private fun rowView(iconView: View, titleView: TextView, subtitle: String?, onClick: () -> Unit): View {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        v.addView(iconView, LinearLayout.LayoutParams(dp(40), dp(40)))
        val mid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(titleView, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        if (subtitle != null) {
            mid.addView(Ui.text(this, subtitle, 13f, R.color.textSecondary),
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
        }
        v.addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
        v.setOnClickListener { onClick() }
        return v
    }

    private fun load() {
        Api.get("/users/me") { code, body ->
            runOnUiThread {
                if (code != 200) return@runOnUiThread
                val j = Api.parseObj(body) ?: return@runOnUiThread
                val u = User.fromJson(j)
                Store.user = u
                usernameTitle.text = "@${u.username}"
                val parts = (u.displayName ?: u.username).split(" ", limit = 2)
                firstInput.setText(parts.getOrNull(0) ?: "")
                lastInput.setText(parts.getOrNull(1) ?: "")
                bioInput.setText(u.bio ?: "")
                bioCounter.text = "${70 - (u.bio?.length ?: 0)}"
            }
        }
    }

    private fun saveProfile() {
        val first = firstInput.text.toString().trim()
        val last = lastInput.text.toString().trim()
        val dn = listOf(first, last).filter { it.isNotEmpty() }.joinToString(" ")
        val bio = bioInput.text.toString().trim()
        Api.patch("/users/me", JSONObject().put("displayName", dn).put("bio", bio)) { code, body ->
            runOnUiThread {
                if (code in 200..299) {
                    val j = Api.parseObj(body)
                    if (j != null) Store.user = User.fromJson(j)
                    Ui.snackbar(this, "Сохранено ✓")
                    finish()
                } else {
                    NxDialog(this).title("Ошибка").message(Api.friendlyError(body)).button("OK") {}.show()
                }
            }
        }
    }

    private fun showStats() {
        Api.get("/users/me/stats") { code, body ->
            runOnUiThread {
                if (code != 200) {
                    Ui.snackbar(this, "Статистика недоступна")
                    return@runOnUiThread
                }
                val j = Api.parseObj(body) ?: return@runOnUiThread
                val names = mapOf(
                    "messages" to "Сообщений",
                    "chats" to "Чатов",
                    "reactions" to "Реакций",
                    "bots" to "Ботов",
                    "attachments" to "Вложений"
                )
                val lines = mutableListOf<String>()
                val keys = j.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = j.opt(k)
                    if (v is Number) lines.add("${names[k] ?: k}: $v")
                }
                if (lines.isEmpty()) lines.add("Пока нет данных")
                NxDialog(this).title("Моя статистика").items(lines) {}.show()
            }
        }
    }
}