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
import com.nexus.messenger.data.Store
import com.nexus.messenger.ui.BottomNav
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp

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

        // Шапка профиля
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
        head.addView(avatarWrap, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        head.addView(
            Ui.text(this, Store.user?.username ?: "Гость", 22f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(14) }
        )
        head.addView(
            Ui.text(this, "@${Store.user?.username ?: "—"}", 14f, R.color.textSecondary),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(2) }
        )
        content.addView(head, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val soon = { Toast.makeText(this, "Появится в следующем обновлении", Toast.LENGTH_SHORT).show() }

        addCard(content, listOf(
            row(R.drawable.ic_person, BLUE, "Аккаунт", "Имя, пользователь, «О себе»", onClick = {
                startActivity(Intent(this, ProfileActivity::class.java))
            }),
            row(R.drawable.ic_chat, ORANGE, "Настройки чатов", "Обои, ночной режим, анимации", onClick = soon),
            row(R.drawable.ic_key, GREEN, "Конфиденциальность", "Время захода, устройства, ключи", onClick = soon),
            row(R.drawable.ic_bell, PINK, "Уведомления", "Звуки, звонки, счётчик сообщений", onClick = soon),
            row(R.drawable.ic_data, INDIGO, "Данные и память", "Настройки загрузки медиафайлов", onClick = soon),
            row(R.drawable.ic_folder, CYAN, "Папки с чатами", "Сортировка чатов по папкам", onClick = soon),
            row(R.drawable.ic_device, CYAN, "Устройства", "Управление активными сеансами", onClick = soon),
            row(R.drawable.ic_globe, PURPLE, "Язык", "Русский", onClick = soon)
        ))

        addCard(content, listOf(
            row(R.drawable.ic_bot, ORANGE, "Боты", "Создание и управление ботами", onClick = {
                startActivity(Intent(this, BotsActivity::class.java))
            }),
            row(R.drawable.ic_star, PURPLE, "Мотесы", "Галерея мотесов", onClick = soon),
            row(R.drawable.ic_palette, PINK, "Темы оформления", "Цветовые темы интерфейса", onClick = soon),
            row(R.drawable.ic_grid, INDIGO, "Иконка приложения", "Смена иконки в лаунчере", onClick = {
                startActivity(Intent(this, IconPickerActivity::class.java))
            }, trailing = ImageView(this).apply {
                setImageResource(IconManager.getCurrent(this@SettingsActivity).drawable)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }),
            row(R.drawable.ic_device, RED, "Сервер", Store.apiBase, onClick = { showServerDialog() })
        ))

        addCard(content, listOf(
            row(R.drawable.ic_close, RED, "Выйти", null, onClick = {
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
                topMargin = dp(20)
                gravity = Gravity.CENTER_HORIZONTAL
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
        titleColorRes: Int = R.color.textPrimary,
        trailing: View? = null
    ): View {
        val ctx = this
        val v = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        v.addView(Ui.tileIcon(ctx, iconRes, tileColor), LinearLayout.LayoutParams(dp(40), dp(40)))
        val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        // ВАЖНО: mid вертикальный, поэтому ширина детей MATCH_PARENT, а не 0+weight
        mid.addView(
            Ui.text(ctx, title, 16f, titleColorRes),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        )
        if (subtitle != null) {
            mid.addView(
                Ui.text(ctx, subtitle, 13f, R.color.textSecondary),
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) }
            )
        }
        v.addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
        if (trailing != null) {
            v.addView(trailing, LinearLayout.LayoutParams(dp(32), dp(32)))
        }
        v.setOnClickListener { onClick() }
        return v
    }

    private fun addCard(parent: LinearLayout, rows: List<View>) {
        val ctx = this@SettingsActivity
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(ctx)
        }
        rows.forEachIndexed { i, r ->
            card.addView(r, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            if (i < rows.size - 1) {
                card.addView(
                    View(ctx).apply { setBackgroundColor(color(R.color.divider)) },
                    LinearLayout.LayoutParams(MATCH_PARENT, 1).apply { leftMargin = dp(72) }
                )
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