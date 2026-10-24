package io.github.gutsmcd.offlinebrowser

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SavedActivity : Activity() {
    private lateinit var store: Store
    private lateinit var list: LinearLayout
    private lateinit var search: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.load(this); store = Store(this)
        val pad = Ui.dp(this, 12f)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.BG); setPadding(pad, pad, pad, 0) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(Ui.button(this, "←", false) { finish() }.apply { contentDescription = Ui.t("戻る", "Back") })
        top.addView(Ui.text(this, Ui.t("  保存したページ", "  Saved pages"), 20f, Ui.GREEN, true))
        root.addView(top)
        search = EditText(this).apply {
            hint = Ui.t("タイトル・本文・アドレスで検索", "Search titles, text, addresses"); setSingleLine()
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Ui.sp(15f)); minHeight = Ui.dp(context, 48f)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) = refresh()
            })
        }
        root.addView(search)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        list.removeAllViews()
        val q = search.text.toString()
        val items = store.all().filter { Logic.matches(it.title, it.url, if (q.isBlank()) "" else store.text(it.id), q) }
        if (items.isEmpty()) {
            list.addView(Ui.text(this, if (q.isBlank()) Ui.t("まだありません。ページを開いて「⬇ 保存」を押すか、他のアプリの「共有」からこのアプリを選んでください。",
                "Nothing yet. Open a page and tap “⬇ Save”, or use Share → Offline Browser in any app.") else Ui.t("見つかりません", "No matches"), 15f, Ui.DIM)
                .apply { setPadding(0, Ui.dp(context, 24f), 0, 0) })
            return
        }
        val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
        val m = Ui.dp(this, 6f)
        for (s in items) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(m * 2, m * 2, m * 2, m * 2)
                background = Ui.round(Ui.CARD, Ui.dp(context, 14f).toFloat(), Ui.dp(context, 1f), Ui.PINK_SOFT)
            }
            card.addView(Ui.text(this, s.title, 16f, Ui.INK, true).apply { maxLines = 3 })
            card.addView(Ui.text(this, fmt.format(Date(s.time)) + " · " + Logic.hostOf(s.url) + (if (s.viaLink) Ui.t(" · リンク先", " · linked") else ""), 13f, Ui.DIM))
            val row = LinearLayout(this).apply { setPadding(0, m, 0, 0) }
            fun gap() = View(this).apply { layoutParams = LinearLayout.LayoutParams(m, 1) }
            row.addView(Ui.button(this, Ui.t("開く", "Open"), true) { open(s, false) }); row.addView(gap())
            row.addView(Ui.button(this, Ui.t("文字だけ", "Text only"), false) { open(s, true) }); row.addView(gap())
            row.addView(Ui.button(this, Ui.t("削除", "Delete"), false) {
                AlertDialog.Builder(this).setMessage(Ui.t("「${s.title}」を削除しますか？", "Delete “${s.title}”?"))
                    .setPositiveButton(Ui.t("削除", "Delete")) { _, _ -> store.delete(s.id); refresh() }
                    .setNegativeButton(Ui.t("やめる", "Cancel"), null).show()
            })
            card.addView(row)
            card.setOnClickListener { open(s, false) }
            list.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, m, 0, m) })
        }
    }

    private fun open(s: Saved, textOnly: Boolean) =
        startActivity(Intent(this, ReaderActivity::class.java).putExtra("id", s.id).putExtra("title", s.title).putExtra("text", textOnly))
}
