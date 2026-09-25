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
import com.nexus.messenger.ui.BottomNav
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONObject

class SettingsActivity : Activity() {

    private val BLUE = 0xFF4D7EC2.toInt()
    private val ORANGE = 0xFFE09A3F.toInt()
    private val GREEN = 0xFF4CD964.toInt()
    private val PINK = 0xFFE05576.toInt()
    private val INDIGO = 0xFF7B6FE0.toInt()
    private val CYAN = 0xFF3FC1C9.toInt()
    private val PURPLE = 0xFF9A6FE0.toInt()
    private val RED = 0xFFE05252.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this)
        frame.setBackgroundColor(color(R.color.bgPrimary))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(120))
        }

        // ── Шапка профиля ──
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
            background = Ui.tileCircle(this@SettingsActivity, 0xFF65AADD.toInt())
        }
        avatarWrap.addView(avatar, FrameLayout.LayoutParams(dp(96), dp(96)))
        val camBadge = ImageView(this).apply {
            setImageResource(R.drawable.ic_camera)
            imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
            background = Ui.pill(this@SettingsActivity, R.color.accent)
            setPadding(dp(7), dp(7), dp(7), dp(7))
        }
        avatarWrap.addView(camBadge, FrameLayout.LayoutParams(dp(30), dp(30)).apply {
            gravity = Gravity.BOTTOM or Gravity.END
        })
        head.addView(avatarWrap)
        head.addView(
            Ui.text(this, Store.user?.username ?: "Гость", 22f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(14) }
        )
        head.addView(
            Ui.text(this, "@${Store.user?.username ?: "—"}", 14f, R.color.textSecondary),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(2) }
        )
        content.addView(head, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // ── Карточка 1: основные разделы ──
        val soon = { Toast.makeText(this, "Появится в следующем обновлении", Toast.LENGTH_SHORT).show() }
        addCard(content, listOf(
            row(R.drawable.ic_person, BLUE, "Аккаунт", "Имя, пользователь, «О себе»") {
                startActivity(Intent(this, ProfileActivity::class.java))
            },
            row(R.drawable.ic_chat, ORANGE, "Настройки чатов", "Обои, ночной режим, анимации", soon),
            row(R.drawable.ic_key, GREEN, "Конфиденциальность", "Время захода, устройства, ключи", soon),
            row(R.drawable.ic_bell, PINK, "Уведомления", "Звуки, звонки, счётчик сообщений", soon),
            row(R.drawable.ic_data, INDIGO, "Данные и память", "Настройки загрузки медиафайлов", soon),
            row(R.drawable.ic_folder, CYAN, "Папки с чатами", "Сортировка чатов по папкам", soon),
            row(R.drawable.ic_device, CYAN, "Устройства", "Управление активными сеансами", soon),
            row(R.drawable.ic_globe, PURPLE, "Язык", "Русский", soon)
        ))

        // ── Карточка 2: фирменное Nexus ──
        addCard(content, listOf(
            row(R.drawable.ic_bot, ORANGE, "Боты", "Создание и управление ботами") {
                startActivity(Intent(this, BotsActivity::class.java))
            },
            row(R.drawable.ic_star, PURPLE, "Мотесы", "Галерея мотесов", soon),
            row(R.drawable.ic_palette, PINK, "Темы оформления", "Цветовые темы интерфейса", soon),
            row(R.drawable.ic_grid, INDIGO, "Иконка приложения", "Смена иконки в лаунчере") {
                startActivity(Intent(this, IconPickerActivity::class.java))
            },
            row(R.drawable.ic_device, RED, "Сервер", Store.apiBase) { showServerDialog() }
        ))

        // ── Карточка 3: выход ──
        addCard(content, listOf(
            row(R.drawable.ic_close, RED, "Выйти", null, {
                AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setMessage("Выйти из аккаунта?")
                    .setPositiveButton("Выйти") { _, _ ->
                        Store.logout()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finishAffinity()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }, titleColorRes = R.color.danger)
        ))

        content.addView(
            Ui.text(this, "Nexus Messenger v1.0.0", 12f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = dp(20); gravity = Gravity.CENTER_HORIZONTAL
            }
        )

        scroll.addView(content)
        frame.addView(scroll, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        BottomNav.attach(frame, this, "settings")
        setContentView(frame)
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun row(
        iconRes: Int,
        tileColor: Int,
        title: String,
        subtitle: String?,
        onClick: () -> Unit,
        titleColorRes: Int = R.color.textPrimary
    ): View {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        v.addView(Ui.tileIcon(this, iconRes, tileColor),
            LinearLayout.LayoutParams(dp(40), dp(40)))
        val mid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(Ui.text(this, title, 16f, titleColorRes),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        if (subtitle != null) {
            mid.addView(Ui.text(this, subtitle, 13f, R.color.textSecondary),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { topMargin = dp(2) })
        }
        v.addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
        v.setOnClickListener { onClick() }
        return v
    }

    private fun addCard(parent: LinearLayout, rows: List<View>) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this)
        }
        rows.forEachIndexed { i, r ->
            card.addView(r, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            if (i < rows.size - 1) {
                card.addView(View(this).apply {
                    setBackgroundColor(color(R.color.divider))
                }, LinearLayout.LayoutParams(MATCH_PARENT, 1).apply {
                    leftMargin = dp(72)
                })
            }
        }
        parent.addView(card, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(8)
        })
    }

    private fun showServerDialog() {
        val input = EditText(this).apply {
            setText(Store.apiBase)
            setTextColor(color(R.color.textPrimary))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Адрес сервера")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                Store.apiBase = input.text.toString().trim()
                Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
