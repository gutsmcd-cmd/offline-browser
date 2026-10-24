package io.github.gutsmcd.offlinebrowser

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.ScrollView

/** Opens a saved page with no signal: the full .mht copy, or just its reading text. */
class ReaderActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.load(this)
        val store = Store(this)
        val id = intent.getStringExtra("id") ?: return finish()
        var textOnly = intent.getBooleanExtra("text", false) || !store.mht(id).exists()
        val pad = Ui.dp(this, 8f)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.BG) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(pad, pad, pad, pad) }
        top.addView(Ui.button(this, "←", false) { finish() }.apply { contentDescription = Ui.t("戻る", "Back") })
        top.addView(Ui.text(this, intent.getStringExtra("title") ?: "", 15f, Ui.INK, true).apply { maxLines = 1; setPadding(pad, 0, pad, 0) },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val web = WebView(this).apply {
            settings.allowFileAccess = true          // only our own saved files
            settings.javaScriptEnabled = false       // saved copies are read-only
            settings.builtInZoomControls = true; settings.displayZoomControls = false
            settings.textZoom = if (Ui.big) 125 else 100
        }
        val text = ScrollView(this).apply {
            addView(Ui.text(context, store.text(id), 18f).apply { setPadding(pad * 2, pad, pad * 2, pad * 4); setTextIsSelectable(true); setLineSpacing(0f, 1.3f) })
        }
        lateinit var toggle: android.widget.Button
        fun show() {
            web.visibility = if (textOnly) View.GONE else View.VISIBLE
            text.visibility = if (textOnly) View.VISIBLE else View.GONE
            toggle.text = if (textOnly) Ui.t("ページ", "Page") else Ui.t("文字だけ", "Text only")
            if (!textOnly && web.url == null) web.loadUrl("file://" + store.mht(id).absolutePath)
        }
        toggle = Ui.button(this, "", false) { textOnly = !textOnly; show() }
        top.addView(toggle)
        root.addView(top)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        root.addView(web, lp); root.addView(text, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        show()
    }
}
