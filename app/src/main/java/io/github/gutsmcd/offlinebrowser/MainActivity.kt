package io.github.gutsmcd.offlinebrowser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var helper: WebView
    private lateinit var address: EditText
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var store: Store
    private val main = Handler(Looper.getMainLooper())
    private var autoSave = false
    private var queue = ArrayDeque<String>()
    private var queueTotal = 0
    private var queueDone = 0

    companion object {
        const val TEXT_JS = "(function(){var m=document.querySelector('article')||document.querySelector('main')||document.body;" +
            "return (document.title||'')+'\\n\\n'+(m?m.innerText:'');})()"
        const val LINKS_JS = "(function(){return Array.prototype.map.call(document.querySelectorAll('a[href]'),function(a){return a.href;});})()"

        fun jsString(r: String?): String = try { JSONArray("[$r]").optString(0, "") } catch (e: Exception) { "" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.load(this); store = Store(this)
        build()
        if (!handleShare(intent)) {
            val last = savedInstanceState?.getString("url")
            if (last != null) web.loadUrl(last) else web.loadUrl("https://duckduckgo.com/")
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleShare(intent) }

    override fun onSaveInstanceState(out: Bundle) { super.onSaveInstanceState(out); web.url?.let { out.putString("url", it) } }

    private fun handleShare(i: Intent?): Boolean {
        if (i?.action != Intent.ACTION_SEND) return false
        val url = Logic.extractUrl(i.getStringExtra(Intent.EXTRA_TEXT)) ?: run {
            toast(Ui.t("リンクが見つかりませんでした", "No link found")); return false
        }
        autoSave = true
        status(Ui.t("開いて保存します…", "Opening and saving…"))
        web.loadUrl(url)
        return true
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setup(w: WebView) = w.apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.textZoom = if (Ui.big) 125 else 100
    }

    private fun build() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.BG) }
        val pad = Ui.dp(this, 6f)

        val row1 = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(pad, pad, pad, 0) }
        row1.addView(Ui.button(this, "←", false) { if (web.canGoBack()) web.goBack() }.apply { contentDescription = Ui.t("戻る", "Back") })
        row1.addView(Ui.button(this, "→", false) { if (web.canGoForward()) web.goForward() }.apply { contentDescription = Ui.t("進む", "Forward") })
        address = EditText(this).apply {
            hint = Ui.t("アドレスか検索ワード", "Address or search")
            setSingleLine(); imeOptions = EditorInfo.IME_ACTION_GO
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Ui.sp(15f)); setTextColor(Ui.INK)
            background = Ui.round(Ui.CARD, Ui.dp(context, 20f).toFloat(), Ui.dp(context, 1f), Ui.GREEN)
            setPadding(Ui.dp(context, 14f), 0, Ui.dp(context, 14f), 0); minHeight = Ui.dp(context, 48f)
            setOnEditorActionListener { _, a, e ->
                if (a == EditorInfo.IME_ACTION_GO || e?.keyCode == KeyEvent.KEYCODE_ENTER) { go(); true } else false
            }
        }
        row1.addView(address, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(pad, 0, pad, 0) })
        row1.addView(Ui.button(this, "⟳", false) { web.reload() }.apply { contentDescription = Ui.t("再読み込み", "Reload") })
        root.addView(row1)

        val row2 = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(pad, pad, pad, pad) }
        fun gap() = View(this).apply { layoutParams = LinearLayout.LayoutParams(pad, 1) }
        row2.addView(Ui.button(this, Ui.t("⬇ 保存", "⬇ Save"), true) { saveCurrent() }); row2.addView(gap())
        row2.addView(Ui.button(this, Ui.t("📚 保存リスト", "📚 Saved"), false) { startActivity(Intent(this, SavedActivity::class.java)) }); row2.addView(gap())
        row2.addView(Ui.button(this, Ui.t("リンク先も保存", "Save + links"), false) { saveWithLinks() }); row2.addView(gap())
        row2.addView(Ui.button(this, if (Ui.big) "あ ✓" else "あ", false) { Ui.big = !Ui.big; Ui.save(this); recreate() }
            .apply { contentDescription = Ui.t("大きい文字", "Large text") }); row2.addView(gap())
        row2.addView(Ui.button(this, if (Ui.ja) "English" else "日本語", false) { Ui.ja = !Ui.ja; Ui.save(this); recreate() })
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(row2) })

        status = Ui.text(this, "", 13f, Ui.GREEN).apply { setPadding(pad * 2, 0, pad * 2, pad); visibility = View.GONE }
        root.addView(status)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        root.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 4f)))

        web = setup(WebView(this))
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(v: WebView, url: String, favicon: Bitmap?) { address.setText(url) }
            override fun onPageFinished(v: WebView, url: String) {
                address.setText(url)
                if (autoSave) { autoSave = false; main.postDelayed({ saveCurrent() }, 1200) }
            }
            override fun onReceivedError(v: WebView, req: WebResourceRequest, err: WebResourceError) {
                if (req.isForMainFrame) {
                    autoSave = false
                    status(Ui.t("開けません（電波なし？）。保存したページは「保存リスト」で読めます。",
                        "Can't open this (no signal?). Saved pages are in “Saved”."))
                }
            }
        }
        web.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(v: WebView, p: Int) { progress.progress = p; progress.visibility = if (p >= 100) View.INVISIBLE else View.VISIBLE }
        }
        root.addView(web, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // Off-screen helper for "save + links" so the page you are reading does not change.
        helper = setup(WebView(this)).apply { visibility = View.INVISIBLE }
        helper.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView, url: String) { main.postDelayed({ saveHelper(url) }, 1500) }
        }
        root.addView(helper, LinearLayout.LayoutParams(Ui.dp(this, 360f), 1))
        setContentView(root)
    }

    private fun go() {
        web.loadUrl(Logic.toUrl(address.text.toString()))
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(address.windowToken, 0)
        web.requestFocus()
    }

    private fun save(w: WebView, viaLink: Boolean, done: (Boolean) -> Unit) {
        val url = w.url ?: return done(false)
        if (!url.startsWith("http")) return done(false)
        val id = store.newId()
        val title = w.title?.takeIf { it.isNotBlank() } ?: url
        w.saveWebArchive(store.mht(id).absolutePath, false) { path ->
            if (path == null) { done(false); return@saveWebArchive }
            w.evaluateJavascript(TEXT_JS) { r ->
                store.add(Saved(id, title, url, System.currentTimeMillis(), viaLink), jsString(r))
                done(true)
            }
        }
    }

    private fun saveCurrent() {
        save(web, false) { ok ->
            status(if (ok) Ui.t("✓ 保存しました：電波がなくても読めます", "✓ Saved: you can read it with no signal")
                   else Ui.t("このページは保存できませんでした", "Couldn't save this page"))
        }
    }

    private fun saveWithLinks() {
        if (queue.isNotEmpty()) { toast(Ui.t("保存中です…", "Already saving…")); return }
        val page = web.url ?: return
        save(web, false) { }
        web.evaluateJavascript(LINKS_JS) { r ->
            val arr = try { JSONArray(r) } catch (e: Exception) { JSONArray() }
            val links = Logic.linksToSave(page, (0 until arr.length()).map { arr.optString(it) })
            if (links.isEmpty()) { status(Ui.t("✓ 保存しました（リンク先はありません）", "✓ Saved (no links to follow)")); return@evaluateJavascript }
            queue = ArrayDeque(links); queueTotal = links.size; queueDone = 0
            next()
        }
    }

    private fun next() {
        val u = queue.firstOrNull()
        if (u == null) {
            status(Ui.t("✓ このページとリンク先 $queueDone 件を保存しました", "✓ Saved this page and $queueDone linked pages")); return
        }
        status(Ui.t("リンク先を保存中 ${queueTotal - queue.size + 1}/$queueTotal …", "Saving linked pages ${queueTotal - queue.size + 1}/$queueTotal …"))
        helper.loadUrl(u)
        // Give up on a page that never finishes loading.
        val tag = u
        main.postDelayed({ if (queue.firstOrNull() == tag) { queue.removeFirstOrNull(); next() } }, 25000)
    }

    private fun saveHelper(url: String) {
        if (queue.isEmpty() || helper.url != url) return
        save(helper, true) { ok -> if (ok) queueDone++; queue.removeFirstOrNull(); next() }
    }

    private fun status(s: String) { status.text = s; status.visibility = View.VISIBLE }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    // Back goes back in the page history first (gesture back on Android 13+, back key on older phones).
    private val backCb by lazy { android.window.OnBackInvokedCallback { if (web.canGoBack()) web.goBack() else finish() } }

    override fun onResume() {
        super.onResume()
        if (android.os.Build.VERSION.SDK_INT >= 33)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCb)
    }

    override fun onPause() {
        super.onPause()
        if (android.os.Build.VERSION.SDK_INT >= 33) onBackInvokedDispatcher.unregisterOnBackInvokedCallback(backCb)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()) { web.goBack(); return true }
        return super.onKeyDown(keyCode, event)
    }
}
