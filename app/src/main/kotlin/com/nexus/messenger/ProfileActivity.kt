package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
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
import android.widget.Toast
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Store
import com.nexus.messenger.data.User
import com.nexus.messenger.ui.BottomNav
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONObject

class ProfileActivity : Activity() {
    private lateinit var nameTv: TextView
    private lateinit var subTv: TextView
    private lateinit var bioValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this)
        frame.setBackgroundColor(color(R.color.bgPrimary))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(120))
        }

        // Шапка
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(24), dp(16), dp(20))
        }
        val avatarWrap = FrameLayout(this)
        val avatar = TextView(this).apply {
            text = (Store.user?.username ?: "?").take(1).uppercase()
            textSize = 36f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            background = Ui.tileCircle(this@ProfileActivity, 0xFF65AADD.toInt())
        }
        avatarWrap.addView(avatar, FrameLayout.LayoutParams(dp(96), dp(96)))
        val camBadge = ImageView(this).apply {
            setImageResource(R.drawable.ic_camera)
            imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
            background = Ui.pill(this@ProfileActivity, R.color.accent)
            setPadding(dp(7), dp(7), dp(7), dp(7))
        }
        camBadge.setOnClickListener { Toast.makeText(this@ProfileActivity, "Загрузка фото появится позже", Toast.LENGTH_SHORT).show() }
        avatarWrap.addView(camBadge, FrameLayout.LayoutParams(dp(30), dp(30)).apply {
            gravity = Gravity.BOTTOM or Gravity.END
        })
        head.addView(avatarWrap, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        nameTv = Ui.text(this, Store.user?.username ?: "—", 22f, R.color.textPrimary, true)
        head.addView(nameTv, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(14) })
        subTv = Ui.text(this, "@${Store.user?.username ?: "—"}", 14f, R.color.textSecondary)
        head.addView(subTv, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(2) })
        content.addView(head, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // Три кнопки-карточки
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), 0, dp(4), 0)
        }
        actions.addView(actionCard(R.drawable.ic_camera, "Выбрать фото") {
            Toast.makeText(this, "Загрузка фото появится позже", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        actions.addView(actionCard(R.drawable.ic_pencil, "Изменить") { showEditDialog() },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        actions.addView(actionCard(R.drawable.ic_gear, "Настройки") {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        content.addView(actions, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(4) })

        // Карточка информации
        bioValue = Ui.text(this, "—", 16f, R.color.textPrimary)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@ProfileActivity)
        }
        card.addView(infoRow("Имя пользователя", "@${Store.user?.username ?: "—"}"),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        card.addView(View(this).apply { setBackgroundColor(color(R.color.divider)) },
            LinearLayout.LayoutParams(MATCH_PARENT, 1).apply { leftMargin = dp(16) })
        val bioRow = infoRowView("О себе", bioValue)
        card.addView(bioRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        content.addView(card, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) })

        scroll.addView(content)
        frame.addView(scroll, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        BottomNav.attach(frame, this, "profile")
        setContentView(frame)

        load()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun actionCard(iconRes: Int, label: String, onClick: () -> Unit): View {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = Ui.card(this@ProfileActivity)
            setPadding(dp(8), dp(14), dp(8), dp(12))
        }
        v.addView(Ui.icon(this, iconRes, color(R.color.textPrimary)),
            LinearLayout.LayoutParams(dp(24), dp(24)))
        v.addView(Ui.text(this, label, 12f, R.color.textPrimary),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(8) })
        v.setOnClickListener { onClick() }
        return v
    }

    private fun infoRow(label: String, value: String): View =
        infoRowView(label, Ui.text(this, value, 16f, R.color.textPrimary))

    private fun infoRowView(label: String, valueView: TextView): View {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        v.addView(Ui.text(this, label, 12f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        v.addView(valueView, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(4) })
        return v
    }

    private fun load() {
        Api.get("/users/me") { code, body ->
            runOnUiThread {
                if (code == 200) {
                    val j = Api.parseObj(body) ?: return@runOnUiThread
                    val u = User.fromJson(j)
                    Store.user = u
                    nameTv.text = u.displayName ?: u.username
                    subTv.text = "@${u.username}"
                    bioValue.text = u.bio ?: "—"
                }
            }
        }
    }

    private fun showEditDialog() {
        val nameInput = EditText(this).apply {
            hint = "Отображаемое имя"
            setTextColor(color(R.color.textPrimary))
            setHintTextColor(color(R.color.textMuted))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setText(Store.user?.displayName ?: "")
        }
        val bioInput = EditText(this).apply {
            hint = "О себе"
            setTextColor(color(R.color.textPrimary))
            setHintTextColor(color(R.color.textMuted))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setText(Store.user?.bio ?: "")
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), 0)
        }
        box.addView(nameInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        box.addView(bioInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })

        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Редактировать профиль")
            .setView(box)
            .setPositiveButton("Сохранить") { _, _ ->
                Api.patch("/users/me", JSONObject()
                    .put("displayName", nameInput.text.toString().trim())
                    .put("bio", bioInput.text.toString().trim())
                ) { code, _ ->
                    runOnUiThread {
                        if (code == 200) {
                            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
                            load()
                        } else {
                            Toast.makeText(this, "Не удалось сохранить", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}