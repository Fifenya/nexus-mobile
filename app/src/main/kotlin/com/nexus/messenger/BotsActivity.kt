package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.nexus.messenger.data.Api
import org.json.JSONObject

class BotsActivity : Activity() {
    private val bots = mutableListOf<Pair<String, String>>()
    private lateinit var list: ListView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(16, 32, 16, 32)
        }
        val back = Button(this).apply {
            text = "←"; setBackgroundColor(0); setTextColor(resources.getColor(R.color.accent, null)); textSize = 22f
        }
        back.setOnClickListener { finish() }
        header.addView(back)
        header.addView(TextView(this).apply {
            text = "Боты"; textSize = 20f; setTextColor(resources.getColor(R.color.textPrimary, null))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 16 })
        val add = Button(this).apply {
            text = "+"; setBackgroundColor(0); setTextColor(resources.getColor(R.color.accent, null)); textSize = 26f
        }
        add.setOnClickListener { createBot() }
        header.addView(add)
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        list = ListView(this).apply { setBackgroundColor(resources.getColor(R.color.bgPrimary, null)) }
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        list.adapter = adapter
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        emptyText = TextView(this).apply {
            text = "У вас пока нет ботов"; gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textMuted, null))
            visibility = View.GONE
        }
        root.addView(emptyText)

        list.setOnItemLongClickListener { _, _, pos, _ ->
            val (id, name) = bots[pos]
            AlertDialog.Builder(this)
                .setTitle(name)
                .setItems(arrayOf("🔄 Обновить токен", "🗑️ Удалить")) { _, i ->
                    when (i) {
                        0 -> Api.post("/bots/$id/regenerate-token", JSONObject()) { code, body ->
                            runOnUiThread {
                                val j = Api.parseObj(body)
                                AlertDialog.Builder(this).setTitle("Токен").setMessage(j?.optString("token") ?: body).setPositiveButton("OK", null).show()
                            }
                        }
                        1 -> Api.delete("/bots/$id") { _, _ -> runOnUiThread { loadBots() } }
                    }
                }
                .show()
            true
        }

        setContentView(root)
        loadBots()
    }

    private fun loadBots() {
        Api.get("/bots") { code, body ->
            runOnUiThread {
                bots.clear()
                if (code == 200) {
                    val arr = Api.parseArray(body)
                    for (i in 0 until arr.length()) {
                        val j = arr.getJSONObject(i)
                        bots.add(j.optString("id") to j.optString("name"))
                    }
                    @Suppress("UNCHECKED_CAST")
                    (list.adapter as ArrayAdapter<String>).apply {
                        clear()
                        addAll(bots.map { "🤖 ${it.second}" })
                        notifyDataSetChanged()
                    }
                }
                emptyText.visibility = if (bots.isEmpty()) View.VISIBLE else View.GONE
                list.visibility = if (bots.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun createBot() {
        val input = EditText(this).apply {
            hint = "Имя бота"; setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
        }
        AlertDialog.Builder(this)
            .setTitle("Новый бот").setView(input)
            .setPositiveButton("Создать") { _, _ ->
                Api.post("/bots", JSONObject().put("name", input.text.toString())) { code, body ->
                    runOnUiThread {
                        if (code == 200 || code == 201) {
                            val j = Api.parseObj(body)
                            AlertDialog.Builder(this).setTitle("Бот создан").setMessage("Токен: " + (j?.optString("token") ?: "см. в API")).setPositiveButton("OK", null).show()
                            loadBots()
                        } else {
                            AlertDialog.Builder(this).setMessage(body).setPositiveButton("OK", null).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null).show()
    }
}