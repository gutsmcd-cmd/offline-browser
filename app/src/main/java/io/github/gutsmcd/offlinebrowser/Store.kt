package io.github.gutsmcd.offlinebrowser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Saved(val id: String, val title: String, val url: String, val time: Long, val viaLink: Boolean)

/** Saved pages live in the app's private storage: pages/<id>.mht (full page) + pages/<id>.txt (reading text). */
class Store(c: Context) {
    val dir = File(c.filesDir, "pages").apply { mkdirs() }
    private val index = File(dir, "index.json")

    fun all(): List<Saved> {
        if (!index.exists()) return emptyList()
        val a = try { JSONArray(index.readText()) } catch (e: Exception) { JSONArray() }
        return (0 until a.length()).map { a.getJSONObject(it) }.map {
            Saved(it.getString("id"), it.optString("title"), it.optString("url"), it.optLong("time"), it.optBoolean("link"))
        }.sortedByDescending { it.time }
    }

    private fun write(list: List<Saved>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("id", it.id).put("title", it.title).put("url", it.url).put("time", it.time).put("link", it.viaLink)) }
        index.writeText(a.toString())
    }

    fun newId() = System.currentTimeMillis().toString(36) + (100..999).random()
    fun mht(id: String) = File(dir, "$id.mht")
    fun txt(id: String) = File(dir, "$id.txt")
    fun text(id: String) = txt(id).takeIf { it.exists() }?.readText() ?: ""

    @Synchronized fun add(s: Saved, text: String) {
        txt(s.id).writeText(text)
        // Saving the same address again replaces the old copy.
        val old = all().filter { it.url == s.url }
        old.forEach { mht(it.id).delete(); txt(it.id).delete() }
        write(all().filter { it.url != s.url } + s)
    }

    @Synchronized fun delete(id: String) {
        mht(id).delete(); txt(id).delete()
        write(all().filter { it.id != id })
    }
}
