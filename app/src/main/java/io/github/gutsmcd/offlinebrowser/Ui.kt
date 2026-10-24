package io.github.gutsmcd.offlinebrowser

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView

/** Settings + tiny view builders (no AndroidX). Japanese by default. */
object Ui {
    const val BG = 0xFFFFF7FA.toInt()
    const val CARD = 0xFFFFFFFF.toInt()
    const val GREEN = 0xFF2E7D5B.toInt()
    const val PINK = 0xFFE0457B.toInt()
    const val PINK_SOFT = 0xFFFFE3EE.toInt()
    const val INK = 0xFF1F2A24.toInt()
    const val DIM = 0xFF5B6660.toInt()

    var ja = true
    var big = false

    fun load(c: Context) {
        val p = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        ja = p.getBoolean("ja", true); big = p.getBoolean("big", false)
    }

    fun save(c: Context) {
        c.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("ja", ja).putBoolean("big", big).apply()
    }

    fun t(jaText: String, en: String) = if (ja) jaText else en
    fun sp(v: Float) = if (big) v * 1.25f else v

    fun dp(c: Context, v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.resources.displayMetrics).toInt()

    fun text(c: Context, s: CharSequence, size: Float, color: Int = INK, bold: Boolean = false) = TextView(c).apply {
        text = s; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(size)); setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    fun round(color: Int, radius: Float, stroke: Int = 0, strokeColor: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius
        if (stroke > 0) setStroke(stroke, strokeColor)
    }

    fun button(c: Context, s: String, primary: Boolean, onClick: () -> Unit) = Button(c).apply {
        text = s; isAllCaps = false; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(15f))
        setTextColor(if (primary) Color.WHITE else GREEN)
        background = round(if (primary) PINK else CARD, dp(c, 22f).toFloat(), if (primary) 0 else dp(c, 1f), GREEN)
        minHeight = dp(c, 48f); minWidth = dp(c, 48f); minimumHeight = dp(c, 48f); minimumWidth = dp(c, 48f)
        setPadding(dp(c, 12f), 0, dp(c, 12f), 0)
        setOnClickListener { onClick() }
    }
}
