package com.nexus.messenger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.ImageLoader
import com.nexus.messenger.data.Mote
import com.nexus.messenger.data.Store
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONObject
import java.net.URLEncoder

class MotesActivity : Activity() {
    private lateinit var grid: GridLayout
    private lateinit var search: EditText
    private val motes = mutableListOf<Mote>()
    private var pendingUploadUri: android.net.Uri? = null

    companion object {
        const val PICK_IMAGE = 4101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this)
        frame.setBackgroundColor(color(R.color.bgPrimary))

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(color(R.color.bgSecondary))
            setPadding(dp(4), dp(10), dp(8), dp(10))
        }
        val back = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            imageTintList = ColorStateList.valueOf(color(R.color.accentText))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        back.setOnClickListener { finish() }
        header.addView(back, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        header.addView(Ui.text(this, "Моты", 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
        val add = ImageView(this).apply {
            setImageResource(R.drawable.ic_person_add)
            imageTintList = ColorStateList.valueOf(color(R.color.textSecondary))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        add.setOnClickListener { pickImage() }
        header.addView(add, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        search = EditText(this).apply {
            hint = "Поиск по имени или тегу"
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = Ui.pill(this@MotesActivity, R.color.bgInput)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            textSize = 14f
            maxLines = 1
        }
        root.addView(search, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            leftMargin = dp(12); rightMargin = dp(12); topMargin = dp(8); bottomMargin = dp(8)
        })

        val scroll = ScrollView(this)
        grid = GridLayout(this).apply { columnCount = 3 }
        scroll.addView(grid, ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        frame.addView(root, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(frame)

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { load(s?.toString()?.trim() ?: "") }
        })

        load("")
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(Intent.createChooser(intent, "Выберите картинку"), PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            upload(uri)
        }
    }

    private fun upload(uri: android.net.Uri) {
        Ui.snackbar(this, "Загрузка…")
        Api.uploadFile(this, uri) { code, body ->
            runOnUiThread {
                if (code in 200..299) {
                    val j = Api.parseObj(body)
                    val url = j?.optString("url") ?: ""
                    if (url.isEmpty()) {
                        Ui.snackbar(this, "Сервер не вернул ссылку")
                        return@runOnUiThread
                    }
                    val nameInput = EditText(this).apply {
                        hint = "Название мота"
                        setTextColor(color(R.color.textPrimary))
                        background = Ui.pill(this@MotesActivity, R.color.bgInput)
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                    }
                    NxDialog(this)
                        .title("Новый мот")
                        .view(nameInput)
                        .button("Сохранить") {
                            Api.post("/motes/gallery", JSONObject()
                                .put("name", nameInput.text.toString().trim().ifEmpty { "Мот" })
                                .put("url", url)
                                .put("tags", "")
                            ) { c2, _ ->
                                runOnUiThread {
                                    if (c2 in 200..299) {
                                        Ui.snackbar(this, "Мот добавлен ✓")
                                        load("")
                                    } else {
                                        Ui.snackbar(this, "Не удалось сохранить мот")
                                    }
                                }
                            }
                        }
                        .button("Отмена") {}
                        .show()
                } else {
                    Ui.snackbar(this, Api.friendlyError(body))
                }
            }
        }
    }

    private fun load(query: String) {
        val path = if (query.isEmpty()) "/motes/gallery"
        else "/motes/gallery?search=" + URLEncoder.encode(query, "UTF-8")
        Api.get(path) { code, body ->
            runOnUiThread {
                motes.clear()
                if (code == 200) {
                    val arr = Api.parseArray(body)
                    for (i in 0 until arr.length()) motes.add(Mote.fromJson(arr.getJSONObject(i)))
                }
                render()
            }
        }
    }

    private fun render() {
        grid.removeAllViews()
        if (motes.isEmpty()) {
            grid.addView(Ui.text(this, "Мотов пока нет. Нажмите «+» чтобы загрузить первый.", 14f, R.color.textMuted),
                GridLayout.LayoutParams().apply {
                    width = MATCH_PARENT
                    setMargins(dp(16), dp(40), dp(16), dp(16))
                })
            return
        }
        motes.forEach { mote ->
            val cell = FrameLayout(this)
            val img = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            ImageLoader.load(this, mote.url, img)
            cell.addView(img, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

            val nameTag = TextView(this).apply {
                text = mote.name
                textSize = 10f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0x88000000)
                setPadding(dp(6), dp(3), dp(6), dp(3))
                maxLines = 1
            }
            cell.addView(nameTag, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
            })

            cell.setOnLongClickListener {
                val mine = mote.ownerId == Store.user?.id
                val actions = if (mine) listOf("Удалить мот") else listOf("Мот не ваш")
                NxDialog(this).items(actions) { i ->
                    if (mine && i == 0) {
                        NxDialog(this)
                            .message("Удалить мот «${mote.name}»?")
                            .button("Удалить") {
                                Api.delete("/motes/gallery/${mote.id}") { c2, _ ->
                                    runOnUiThread {
                                        if (c2 in 200..299) {
                                            Ui.snackbar(this, "Мот удалён")
                                            load(search.text.toString().trim())
                                        } else {
                                            Ui.snackbar(this, "Не удалось удалить")
                                        }
                                    }
                                }
                            }
                            .button("Отмена") {}
                            .show()
                    }
                }.show()
                true
            }

            val size = (resources.displayMetrics.widthPixels - dp(24)) / 3
            val params = GridLayout.LayoutParams().apply {
                width = size
                height = size
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
            grid.addView(cell, params)
        }
    }
}